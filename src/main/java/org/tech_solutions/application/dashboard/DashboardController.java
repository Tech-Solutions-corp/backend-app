package org.tech_solutions.application.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tech_solutions.application.dashboard.dto.BalancePerMonthDto;
import org.tech_solutions.application.dashboard.dto.ExpenseByCategoryDto;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/v1/metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/expenses-categories/{id}")
    public ResponseEntity<List<ExpenseByCategoryDto>> listExpensesByCategories(@PathVariable Long id) {
        List<ExpenseByCategoryDto> expensesByCategories = dashboardService.listExpensesByCategories(id);

        if (expensesByCategories.isEmpty()) {
            return ResponseEntity.status(204).body(Collections.emptyList());
        }
        return ResponseEntity.ok(expensesByCategories);
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<List<BalancePerMonthDto>> getBalancePerMonth(@PathVariable Long id) {
        List<BalancePerMonthDto> expensesByCategories = dashboardService.getBalancePerMonth(id);

        if (expensesByCategories.isEmpty()) {
            return ResponseEntity.status(204).body(Collections.emptyList());
        }
        return ResponseEntity.ok(expensesByCategories);
    }
}
