package org.tech_solutions.application.aiinsights.generator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.aiinsights.enums.InsightType;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.service.AiInsightService;
import org.tech_solutions.application.importedtransactions.repository.ImportedTransactionRepository;
import org.tech_solutions.application.imports.service.ImportFileService;
import org.tech_solutions.application.transactions.enums.TransactionType;
import org.tech_solutions.application.transactions.model.Transaction;
import org.tech_solutions.application.transactions.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiInsightGenerator {
        private final TransactionRepository transactionRepository;
        private final ImportedTransactionRepository importedTransactionRepository;
        private final AiInsightService aiInsightService;
        private final ChatClient chatClient;
        private final ImportFileService fileService;

        public AiInsightGenerator(TransactionRepository transactionRepository,
                        ImportedTransactionRepository importedTransactionRepository, AiInsightService aiInsightService,
                        ChatClient chatClient, ImportFileService fileService) {
                this.transactionRepository = transactionRepository;
                this.importedTransactionRepository = importedTransactionRepository;
                this.aiInsightService = aiInsightService;
                this.chatClient = chatClient;
                this.fileService = fileService;
        }

        public AiInsight generateAndSave(Long userId) {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);

                // 1. Coleta dados agregados
                var expensesByCategory = transactionRepository
                                .findExpensesByCategoryAndPeriod(userId, startDate, endDate);
                var incomeVsExpense = transactionRepository
                                .findIncomeVsExpense(userId, startDate, endDate);
                List<Transaction> topExpenses = transactionRepository.findTopExpenses(
                                userId,
                                startDate,
                                endDate,
                                TransactionType.EXPENSE,
                                PageRequest.of(0, 3) // pega apenas os 3 maiores
                );
                List<Object[]> importedSummary = importedTransactionRepository
                                .findRawSummaryByUser(userId, startDate, endDate);
                List<String> fileDetails = fileService
                                .fetchCsvDescriptions(userId, startDate, endDate);

                // 2. Monta o prompt com os dados
                String prompt = buildPrompt(expensesByCategory, incomeVsExpense,
                                topExpenses, importedSummary, fileDetails, startDate, endDate);

                // 3. Chama o LLM
                String insightContent = chatClient.prompt()
                                .user(prompt)
                                .call()
                                .content();

                // 4. Persiste usando o service que já existe
                AiInsight insight = new AiInsight();
                insight.setInsightType(InsightType.SPENDING_PATTERN);
                insight.setContent(insightContent);
                return aiInsightService.create(insight, userId);
        }

        public AiInsight generateAndSaveWithSpecification(Long userId, InsightType insightType, String specification) {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);

                // 1. Coleta dados agregados
                var expensesByCategory = transactionRepository
                                .findExpensesByCategoryAndPeriod(userId, startDate, endDate);
                var incomeVsExpense = transactionRepository
                                .findIncomeVsExpense(userId, startDate, endDate);
                List<Transaction> topExpenses = transactionRepository.findTopExpenses(
                                userId,
                                startDate,
                                endDate,
                                TransactionType.EXPENSE,
                                PageRequest.of(0, 3));
                List<Object[]> importedSummary = importedTransactionRepository
                                .findRawSummaryByUser(userId, startDate, endDate);
                List<String> fileDetails = fileService
                                .fetchCsvDescriptions(userId, startDate, endDate);

                // 2. Monta o prompt com os dados
                String prompt = buildPromptWithSpecification(expensesByCategory, incomeVsExpense,
                                topExpenses, importedSummary, fileDetails, startDate, endDate, insightType, specification);

                // 3. Chama o LLM
                String insightContent = chatClient.prompt()
                                .user(prompt)
                                .call()
                                .content();

                // 4. Persiste usando o service que já existe
                AiInsight insight = new AiInsight();
                insight.setInsightType(insightType);
                insight.setContent(insightContent);
                return aiInsightService.create(insight, userId);
        }

        private String buildPrompt(
                        List<Object[]> expensesByCategory,
                        List<Object[]> incomeVsExpense,
                        List<Transaction> topExpenses,
                        List<Object[]> importedSummary,
                        List<String> fileDetails,
                        LocalDate startDate,
                        LocalDate endDate) {
                // Formata gastos por categoria: "Alimentação: R$350,00 | Transporte: R$120,00"
                String categoriesFormatted = expensesByCategory.stream()
                                .map(row -> "%s: R$%.2f".formatted(row[0], row[1]))
                                .collect(Collectors.joining(" | "));

                // Formata receita vs despesa: busca cada tipo no resultado
                String incomeFormatted = "R$0,00";
                String expenseFormatted = "R$0,00";
                for (Object[] row : incomeVsExpense) {
                        TransactionType type = (TransactionType) row[0];
                        BigDecimal total = (BigDecimal) row[1];
                        if (type == TransactionType.INCOME)
                                incomeFormatted = "R$%.2f".formatted(total);
                        if (type == TransactionType.EXPENSE)
                                expenseFormatted = "R$%.2f".formatted(total);
                }
                String incomeVsExpenseFormatted = "Receita: %s | Despesa: %s".formatted(incomeFormatted,
                                expenseFormatted);

                // Formata top 3 maiores gastos: "Notebook R$3200,00 (2025-03-10)"
                String topExpensesFormatted = topExpenses.stream()
                                .map(t -> "%s R$%.2f (%s)".formatted(
                                                t.getTransactionDescription() != null ? t.getTransactionDescription()
                                                                : "Sem descrição",
                                                t.getAmount(),
                                                t.getTransactionDate()))
                                .collect(Collectors.joining(" | "));

                // Formata resumo das importadas: "15 transações brutas somando R$1.200,00"
                String importedFormatted = "Nenhuma transação importada no período";
                if (importedSummary != null && !importedSummary.isEmpty()) {
                        Object[] row = importedSummary.get(0); // pega a primeira (e única) linha
                        if (row[0] != null) {
                                BigDecimal sum = (BigDecimal) row[0];
                                Long count = (Long) row[1];
                                importedFormatted = "%d transações brutas somando R$%.2f".formatted(count, sum);
                        }
                }

                String fileDetailsFormatted = fileDetails.isEmpty()
                                ? "Nenhum extrato importado disponível"
                                : fileDetails.stream()
                                                .limit(20)
                                                .collect(Collectors.joining("\n"));

                return """
                                Você é um assistente financeiro pessoal integrado a um aplicativo de gestão financeira.
                                Seu objetivo é ajudar o usuário a entender melhor seus hábitos financeiros de forma
                                clara, empática e direta.

                                Analise os dados financeiros abaixo referentes ao período de %s até %s
                                e dirija sua resposta diretamente ao usuário, usando a segunda pessoa do singular (você).

                                Use texto simples, sem formatação Markdown, sem negrito, sem listas com asteriscos ou símbolos.

                                Gastos por categoria: %s
                                Receita total vs Despesa total: %s
                                Maiores gastos individuais: %s
                                Transações importadas - resumo: %s
                                Transações importadas - detalhes do extrato:
                                %s

                                Estruture sua resposta em parágrafos claros e práticos:

                                1. Resumo do seu período
                                Apresente um panorama geral de como foi a saúde financeira do usuário no período, sempre informando e
                                destacando seu saldo final segundo os dados apresentados.

                                2. Principal padrão identificado nos seus gastos
                                Aponte o comportamento financeiro mais relevante identificado nos dados,
                                explicando o impacto que ele pode ter no orçamento do usuário.

                                3. Uma dica prática para você melhorar
                                Ofereça uma sugestão concreta, personalizada com base nos dados apresentados,
                                que o usuário possa aplicar no próximo período.
                                """
                                .formatted(
                                                startDate,
                                                endDate,
                                                categoriesFormatted.isEmpty() ? "Nenhum gasto categorizado no período"
                                                                : categoriesFormatted,
                                                incomeVsExpenseFormatted,
                                                topExpensesFormatted.isEmpty() ? "Nenhum gasto encontrado no período"
                                                                : topExpensesFormatted,
                                                importedFormatted,
                                                fileDetailsFormatted);
        }

        private String buildPromptWithSpecification(
                        List<Object[]> expensesByCategory,
                        List<Object[]> incomeVsExpense,
                        List<Transaction> topExpenses,
                        List<Object[]> importedSummary,
                        List<String> fileDetails,
                        LocalDate startDate,
                        LocalDate endDate,
                        InsightType insightType,
                        String specification) {
                // Formata gastos por categoria
                String categoriesFormatted = expensesByCategory.stream()
                                .map(row -> "%s: R$%.2f".formatted(row[0], row[1]))
                                .collect(Collectors.joining(" | "));

                // Formata receita vs despesa
                String incomeFormatted = "R$0,00";
                String expenseFormatted = "R$0,00";
                for (Object[] row : incomeVsExpense) {
                        TransactionType type = (TransactionType) row[0];
                        BigDecimal total = (BigDecimal) row[1];
                        if (type == TransactionType.INCOME)
                                incomeFormatted = "R$%.2f".formatted(total);
                        if (type == TransactionType.EXPENSE)
                                expenseFormatted = "R$%.2f".formatted(total);
                }
                String incomeVsExpenseFormatted = "Receita: %s | Despesa: %s".formatted(incomeFormatted,
                                expenseFormatted);

                // Formata top 3 maiores gastos
                String topExpensesFormatted = topExpenses.stream()
                                .map(t -> "%s R$%.2f (%s)".formatted(
                                                t.getTransactionDescription() != null ? t.getTransactionDescription()
                                                                : "Sem descrição",
                                                t.getAmount(),
                                                t.getTransactionDate()))
                                .collect(Collectors.joining(" | "));

                // Formata resumo das importadas
                String importedFormatted = "Nenhuma transação importada no período";
                if (importedSummary != null && !importedSummary.isEmpty()) {
                        Object[] row = importedSummary.get(0);
                        if (row[0] != null) {
                                BigDecimal sum = (BigDecimal) row[0];
                                Long count = (Long) row[1];
                                importedFormatted = "%d transações brutas somando R$%.2f".formatted(count, sum);
                        }
                }

                String fileDetailsFormatted = fileDetails.isEmpty()
                                ? "Nenhum extrato importado disponível"
                                : fileDetails.stream()
                                                .limit(20)
                                                .collect(Collectors.joining("\n"));

                String basePrompt = """
                                Você é um assistente financeiro pessoal integrado a um aplicativo de gestão financeira.
                                Seu objetivo é ajudar o usuário a entender melhor seus hábitos financeiros de forma
                                clara, empática e direta.

                                Analise os dados financeiros abaixo referentes ao período de %s até %s
                                e responda à seguinte requisição do usuário:
                                "%s"

                                Gastos por categoria: %s
                                Receita total vs Despesa total: %s
                                Maiores gastos individuais: %s
                                Transações importadas - resumo: %s
                                Transações importadas - detalhes do extrato:
                                %s

                                """.formatted(
                                startDate,
                                endDate,
                                specification,
                                categoriesFormatted.isEmpty() ? "Nenhum gasto categorizado no período"
                                                : categoriesFormatted,
                                incomeVsExpenseFormatted,
                                topExpensesFormatted.isEmpty() ? "Nenhum gasto encontrado no período"
                                                : topExpensesFormatted,
                                importedFormatted,
                                fileDetailsFormatted);

                String specificInstructions = switch (insightType) {
                        case SPENDING_PATTERN -> """
                                Foque em identificar padrões nos gastos do usuário. Analise tendências, categorias mais gastas,
                                frequência de gastos e possíveis hábitos recorrentes. Destaque insights sobre como os gastos
                                se distribuem ao longo do tempo e categorias.
                                """;
                        case SAVING_TIP -> """
                                Forneça dicas práticas e personalizadas para economizar dinheiro. Baseie-se nos dados para
                                sugerir cortes em gastos desnecessários, alternativas mais baratas ou estratégias de poupança.
                                Seja específico e acionável.
                                """;
                        case ANOMALY_DETECTION -> """
                                Identifique transações ou padrões incomuns que possam indicar erros, fraudes ou gastos
                                extraordinários. Compare com o histórico normal e destaque qualquer anomalia significativa.
                                """;
                };

                return basePrompt + specificInstructions + """

                                Use texto simples, sem formatação Markdown, sem negrito, sem listas com asteriscos ou símbolos.
                                Dirija sua resposta diretamente ao usuário, usando a segunda pessoa do singular (você).
                                Estruture sua resposta de forma clara, prática e focada na requisição do usuário e no tipo de análise solicitada.
                                """;
        }
}
