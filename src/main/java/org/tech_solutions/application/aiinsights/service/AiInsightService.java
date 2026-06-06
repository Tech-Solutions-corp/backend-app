package org.tech_solutions.application.aiinsights.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.aiinsights.controller.dto.AiInsightHistoricalData;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.repository.AiInsightRepository;
import org.tech_solutions.application.dashboard.dto.BalancePerMonthDto;
import org.tech_solutions.application.dashboard.dto.ExpenseByCategoryDto;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.transactions.repository.TransactionRepository;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ChatClient chatClient;
    private final TransactionRepository transactionRepository;

    public AiInsightService(AiInsightRepository aiInsightRepository, UserRepository userRepository, CurrentUserService currentUserService, ChatClient chatClient, TransactionRepository transactionRepository) {
        this.aiInsightRepository = aiInsightRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.chatClient = chatClient;
        this.transactionRepository = transactionRepository;
    }

    public AiInsight create(AiInsight insight, Long userId) {
        insight.setUser(currentUserService.requireCurrentUser());
        insight.setGeneratedAt(LocalDateTime.now());
        return aiInsightRepository.save(insight);
    }

    public List<AiInsight> listAll() {
        return aiInsightRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<AiInsight> listByUser(Long userId) {
        return aiInsightRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public AiInsight findById(Long id) {
        AiInsight insight = aiInsightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Insight nao encontrado"));
        assertOwnedByCurrentUser(insight);
        return insight;
    }

    public AiInsight update(Long id, AiInsight updated, Long userId) {
        AiInsight current = findById(id);
        current.setUser(currentUserService.requireCurrentUser());
        current.setInsightType(updated.getInsightType());
        current.setContent(updated.getContent());
        return aiInsightRepository.save(current);
    }

    public void delete(Long id) {
        aiInsightRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    private void assertOwnedByCurrentUser(AiInsight insight) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (insight.getUser() == null || !currentUserId.equals(insight.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }

    public List<AiInsightHistoricalData> generateInsightHistoricalData() {

        // 1. Agrega dados do banco
        List<BalancePerMonthDto> balanceHistory = transactionRepository.findAllBalancePerMonth();
        List<ExpenseByCategoryDto> expensesByCategory = transactionRepository.findAllExpensesByCategory();

        // 2. Serializa os agregados em texto para o prompt
        StringBuilder balanceSummary = new StringBuilder();
        for (BalancePerMonthDto b : balanceHistory) {
            balanceSummary.append("Mês %d/%d: receitas R$%.2f, despesas R$%.2f%n"
                    .formatted(b.month(), b.year(), b.income(), b.expense()));
        }

        StringBuilder categorySummary = new StringBuilder();
        for (ExpenseByCategoryDto c : expensesByCategory) {
            categorySummary.append("- %s: R$%.2f%n"
                    .formatted(c.category(), c.totalAmount()));
        }

        // 3. Monta o prompt
        String prompt = """
        Você é um assistente especialista em análise de dados financeiros.
        
        Os dados abaixo são uma combinação de dados históricos provenientes de um dataset externo
        e dados reais de múltiplos usuários da aplicação. Adote uma postura analítica e imparcial,
        identificando padrões, tendências e comportamentos financeiros relevantes com base
        no conjunto completo de dados.
        
        Histórico de receitas e despesas mensais:
        %s
        
        Despesas por categoria (histórico completo):
        %s
        
        Com base nesses dados, gere exatamente 3 insights financeiros relevantes e objetivos.
        
        Diretrizes importantes:
        - NÃO mencione valores monetários específicos nos insights
        - Os insights devem ser sobre padrões e tendências comportamentais
        - Em todo momento deixe explícito que a análise é baseada em dados históricos coletivos de múltiplos usuários da plataforma e de datasets externos, nunca dando a entender que são dados exclusivos de quem está lendo
        - Inicie cada description reforçando que se trata de uma análise coletiva e histórica
        - Cada insight deve ter duas partes: primeiro a análise do padrão identificado, depois uma prescrição prática e objetiva — se a tendência for negativa, oriente como revertê-la; se for positiva, oriente como mantê-la e potencializá-la
        - Evite no começo da description artigos definidos e declarações como "a análise", "os dados", "após análise", substitua essas declarações por declarações semelhantes à "Dados históricos de transações mostram...". "Análises realzadas destacam...", "Nota-se...", etc
        Retorne APENAS um JSON válido, sem texto adicional, sem markdown, no seguinte formato:
        [
          { "title": "...", "description": "..." },
          { "title": "...", "description": "..." },
          { "title": "...", "description": "..." }
        ]
        """.formatted(balanceSummary, categorySummary);

        // 4. Chama a IA
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 5. Parse do JSON retornado
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, new TypeReference<List<AiInsightHistoricalData>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar resposta da IA: " + e.getMessage());
        }
    }
}


