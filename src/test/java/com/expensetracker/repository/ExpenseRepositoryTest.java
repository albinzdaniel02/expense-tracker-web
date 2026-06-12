package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
    }

    @Test
    @DisplayName("sumAllExpenses should return ZERO when no expenses exist")
    void testSumAllExpenses_Empty() {
        BigDecimal total = expenseRepository.sumAllExpenses();
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumAllExpenses should return sum of all expenses")
    void testSumAllExpenses_WithData() {
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("10.50"))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build());

        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("20.00"))
                .category("Transport")
                .expenseDate(LocalDate.now())
                .build());

        BigDecimal total = expenseRepository.sumAllExpenses();
        assertThat(total).isEqualByComparingTo(new BigDecimal("30.50"));
    }

    @Test
    @DisplayName("sumExpensesBetween should return ZERO when no expenses in range")
    void testSumExpensesBetween_Empty() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        BigDecimal total = expenseRepository.sumExpensesBetween(start, end);
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumExpensesBetween should sum only expenses in the date range")
    void testSumExpensesBetween_WithData() {
        // In range
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("15.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 5))
                .build());

        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("25.00"))
                .category("Utilities")
                .expenseDate(LocalDate.of(2026, 6, 15))
                .build());

        // Out of range (before)
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("100.00"))
                .category("Rent")
                .expenseDate(LocalDate.of(2026, 5, 31))
                .build());

        // Out of range (after)
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("50.00"))
                .category("Entertainment")
                .expenseDate(LocalDate.of(2026, 7, 1))
                .build());

        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        BigDecimal total = expenseRepository.sumExpensesBetween(start, end);
        assertThat(total).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("getCategoryBreakdown should aggregate and order categories in range")
    void testGetCategoryBreakdown() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        // Food category (Total: 45.00)
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("15.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 5))
                .build());
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("30.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 10))
                .build());

        // Transport category (Total: 20.00)
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("20.00"))
                .category("Transport")
                .expenseDate(LocalDate.of(2026, 6, 12))
                .build());

        // Food category out of range (ignored)
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 7, 5))
                .build());

        List<ExpenseRepository.CategorySum> breakdown = expenseRepository.getCategoryBreakdown(start, end);

        assertThat(breakdown).hasSize(2);
        
        // Assert ordering and sums
        assertThat(breakdown.get(0).getCategory()).isEqualTo("Food");
        assertThat(breakdown.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("45.00"));

        assertThat(breakdown.get(1).getCategory()).isEqualTo("Transport");
        assertThat(breakdown.get(1).getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
    }
}
