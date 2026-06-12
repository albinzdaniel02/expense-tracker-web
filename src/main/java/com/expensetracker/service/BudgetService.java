package com.expensetracker.service;

import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.repository.BudgetSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    public static final UUID BUDGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final BigDecimal DEFAULT_BUDGET = new BigDecimal("1000.00");

    private final BudgetSettingsRepository budgetSettingsRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (!budgetSettingsRepository.existsById(BUDGET_ID)) {
            seedDefaultBudget();
        }
    }

    @Transactional
    public BudgetSettings getBudgetSettings() {
        return budgetSettingsRepository.findById(BUDGET_ID)
                .orElseGet(this::seedDefaultBudget);
    }

    @Transactional
    public BudgetSettings updateBudgetSettings(BigDecimal newLimit) {
        BudgetSettings settings = budgetSettingsRepository.findById(BUDGET_ID)
                .orElseGet(() -> new BudgetSettings(BUDGET_ID, DEFAULT_BUDGET));
        settings.setMonthlyLimit(newLimit);
        return budgetSettingsRepository.save(settings);
    }

    private BudgetSettings seedDefaultBudget() {
        BudgetSettings defaultSettings = BudgetSettings.builder()
                .id(BUDGET_ID)
                .monthlyLimit(DEFAULT_BUDGET)
                .build();
        return budgetSettingsRepository.save(defaultSettings);
    }
}
