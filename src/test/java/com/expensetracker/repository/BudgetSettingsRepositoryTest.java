package com.expensetracker.repository;

import com.expensetracker.entity.BudgetSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BudgetSettingsRepositoryTest {

    @Autowired
    private BudgetSettingsRepository budgetSettingsRepository;

    @BeforeEach
    void setUp() {
        budgetSettingsRepository.deleteAll();
    }

    @Test
    @DisplayName("should save and retrieve BudgetSettings by ID")
    void testSaveAndFindById() {
        UUID budgetId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BudgetSettings settings = BudgetSettings.builder()
                .id(budgetId)
                .monthlyLimit(new BigDecimal("1500.00"))
                .build();

        budgetSettingsRepository.save(settings);

        Optional<BudgetSettings> found = budgetSettingsRepository.findById(budgetId);
        assertThat(found).isPresent();
        assertThat(found.get().getMonthlyLimit()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }
}
