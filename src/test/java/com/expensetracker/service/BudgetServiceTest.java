package com.expensetracker.service;

import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.repository.BudgetSettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetSettingsRepository budgetSettingsRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("Init should seed default budget when it does not exist")
    void testInit_WhenNotExists() {
        when(budgetSettingsRepository.existsById(BudgetService.BUDGET_ID)).thenReturn(false);
        when(budgetSettingsRepository.save(any(BudgetSettings.class))).thenAnswer(i -> i.getArgument(0));

        budgetService.init();

        verify(budgetSettingsRepository, times(1)).existsById(BudgetService.BUDGET_ID);
        verify(budgetSettingsRepository, times(1)).save(any(BudgetSettings.class));
    }

    @Test
    @DisplayName("Init should not seed default budget when it already exists")
    void testInit_WhenExists() {
        when(budgetSettingsRepository.existsById(BudgetService.BUDGET_ID)).thenReturn(true);

        budgetService.init();

        verify(budgetSettingsRepository, times(1)).existsById(BudgetService.BUDGET_ID);
        verify(budgetSettingsRepository, never()).save(any(BudgetSettings.class));
    }

    @Test
    @DisplayName("Get Budget Settings should retrieve configuration when record exists")
    void testGetBudgetSettings_WhenExists() {
        BudgetSettings expected = new BudgetSettings(BudgetService.BUDGET_ID, new BigDecimal("1500.00"));
        when(budgetSettingsRepository.findById(BudgetService.BUDGET_ID)).thenReturn(Optional.of(expected));

        BudgetSettings actual = budgetService.getBudgetSettings();

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(BudgetService.BUDGET_ID);
        assertThat(actual.getMonthlyLimit()).isEqualTo(new BigDecimal("1500.00"));
        verify(budgetSettingsRepository, never()).save(any(BudgetSettings.class));
    }

    @Test
    @DisplayName("Get Budget Settings should seed and return default config if none exists")
    void testGetBudgetSettings_WhenNotExistsSeedsDefault() {
        when(budgetSettingsRepository.findById(BudgetService.BUDGET_ID)).thenReturn(Optional.empty());
        when(budgetSettingsRepository.save(any(BudgetSettings.class))).thenAnswer(i -> i.getArgument(0));

        BudgetSettings seeded = budgetService.getBudgetSettings();

        assertThat(seeded).isNotNull();
        assertThat(seeded.getId()).isEqualTo(BudgetService.BUDGET_ID);
        assertThat(seeded.getMonthlyLimit()).isEqualTo(new BigDecimal("1000.00"));
        verify(budgetSettingsRepository, times(1)).save(any(BudgetSettings.class));
    }

    @Test
    @DisplayName("Update Budget should retrieve, modify, and save the settings singleton")
    void testUpdateBudgetSettings() {
        BudgetSettings existing = new BudgetSettings(BudgetService.BUDGET_ID, new BigDecimal("1000.00"));
        when(budgetSettingsRepository.findById(BudgetService.BUDGET_ID)).thenReturn(Optional.of(existing));
        when(budgetSettingsRepository.save(any(BudgetSettings.class))).thenAnswer(i -> i.getArgument(0));

        BudgetSettings updated = budgetService.updateBudgetSettings(new BigDecimal("1800.00"));

        assertThat(updated.getMonthlyLimit()).isEqualTo(new BigDecimal("1800.00"));
        verify(budgetSettingsRepository, times(1)).save(existing);
    }
}
