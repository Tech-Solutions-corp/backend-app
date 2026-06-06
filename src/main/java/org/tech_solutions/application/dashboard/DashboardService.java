package org.tech_solutions.application.dashboard;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.dashboard.dto.BalancePerMonthDto;
import org.tech_solutions.application.dashboard.dto.ExpenseByCategoryDto;
import org.tech_solutions.application.transactions.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<ExpenseByCategoryDto> listExpensesByCategories(Long id) {
        return transactionRepository.findExpenseTotalsByCategory(id);
    }

    public List<BalancePerMonthDto> getBalancePerMonth(Long id) {
        LocalDate startDate = YearMonth.now().minusMonths(4).atDay(1);
        List<BalancePerMonthDto> fromDb = transactionRepository.findBalancePerMonth(id, startDate);

        Map<String, BalancePerMonthDto> dbMap = fromDb.stream()
                .collect(Collectors.toMap(
                        dto -> dto.year() + "-" + dto.month(),
                        dto -> dto
                ));

        // Gera os últimos 6 meses
        List<BalancePerMonthDto> result = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 4; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String key = ym.getYear() + "-" + ym.getMonthValue();

            // Merge: usa dado do banco ou zera
            BalancePerMonthDto dto = dbMap.getOrDefault(key, new BalancePerMonthDto(
                    ym.getMonthValue(),
                    ym.getYear(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));

            result.add(dto);
        }

        return result;

    }
}
