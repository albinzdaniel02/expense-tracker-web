package com.expensetracker.controller;

import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/budget-settings")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<BudgetSettings> getBudgetSettings() {
        return ResponseEntity.ok(budgetService.getBudgetSettings());
    }

    @PutMapping
    public ResponseEntity<BudgetSettings> updateBudgetSettings(@Valid @RequestBody Map<String, Object> body) {
        if (!body.containsKey("monthlyLimit") || body.get("monthlyLimit") == null) {
            throw new IllegalArgumentException("Field 'monthlyLimit' is required");
        }
        
        BigDecimal limit;
        try {
            double monthlyLimitDouble = Double.parseDouble(body.get("monthlyLimit").toString());
            limit = BigDecimal.valueOf(monthlyLimitDouble);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid monthlyLimit value");
        }

        if (limit.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Monthly budget limit must be greater than zero");
        }

        return ResponseEntity.ok(budgetService.updateBudgetSettings(limit));
    }
}
