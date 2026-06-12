package com.expensetracker.controller;

import com.expensetracker.entity.Expense;
import com.expensetracker.dto.*;
import com.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String monthYear) {
        return ResponseEntity.ok(expenseService.getExpenses(category, monthYear));
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody Expense expense) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable UUID id, 
            @Valid @RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<SummaryDto> getSummary() {
        return ResponseEntity.ok(expenseService.getSummary());
    }

    @GetMapping("/stats/category-breakdown")
    public ResponseEntity<List<CategorySumDto>> getCategoryBreakdown(
            @RequestParam(required = false) String monthYear) {
        return ResponseEntity.ok(expenseService.getCategoryBreakdown(monthYear));
    }

    @GetMapping("/stats/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDto>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(expenseService.getMonthlyTrends(limit));
    }
}
