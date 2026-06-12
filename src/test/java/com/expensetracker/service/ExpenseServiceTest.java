package com.expensetracker.service;

import com.expensetracker.dto.CategorySumDto;
import com.expensetracker.dto.MonthlyTrendDto;
import com.expensetracker.dto.SummaryDto;
import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.entity.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID sampleId;
    private Expense sampleExpense;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleExpense = Expense.builder()
                .id(sampleId)
                .amount(new BigDecimal("75.50"))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Create Expense should save and return the expense entity")
    void testCreateExpense() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(sampleExpense);

        Expense saved = expenseService.createExpense(sampleExpense);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(sampleId);
        assertThat(saved.getAmount()).isEqualTo(new BigDecimal("75.50"));
        verify(expenseRepository, times(1)).save(sampleExpense);
    }

    @Test
    @DisplayName("Update Expense should modify existing details and save")
    void testUpdateExpense_Success() {
        Expense updatedDetails = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .category("Utilities")
                .expenseDate(LocalDate.now())
                .build();

        when(expenseRepository.findById(sampleId)).thenReturn(Optional.of(sampleExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        Expense result = expenseService.updateExpense(sampleId, updatedDetails);

        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getCategory()).isEqualTo("Utilities");
        verify(expenseRepository, times(1)).findById(sampleId);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Update Expense should throw NoSuchElementException if record is missing")
    void testUpdateExpense_NotFound() {
        when(expenseRepository.findById(sampleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.updateExpense(sampleId, sampleExpense))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Expense not found with ID: " + sampleId);
    }

    @Test
    @DisplayName("Delete Expense should remove record if it exists")
    void testDeleteExpense_Success() {
        when(expenseRepository.existsById(sampleId)).thenReturn(true);
        doNothing().when(expenseRepository).deleteById(sampleId);

        expenseService.deleteExpense(sampleId);

        verify(expenseRepository, times(1)).existsById(sampleId);
        verify(expenseRepository, times(1)).deleteById(sampleId);
    }

    @Test
    @DisplayName("Delete Expense should throw NoSuchElementException if record does not exist")
    void testDeleteExpense_NotFound() {
        when(expenseRepository.existsById(sampleId)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.deleteExpense(sampleId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Expense not found with ID: " + sampleId);
    }

    @Test
    @DisplayName("Get Summary should calculate correct statistics and percentages")
    void testGetSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        BigDecimal mockTotalSpend = new BigDecimal("3500.00");
        BigDecimal mockMonthSpend = new BigDecimal("500.00");
        BudgetSettings mockBudget = new BudgetSettings(BudgetService.BUDGET_ID, new BigDecimal("1000.00"));

        when(expenseRepository.sumAllExpenses()).thenReturn(mockTotalSpend);
        when(expenseRepository.sumExpensesBetween(start, end)).thenReturn(mockMonthSpend);
        when(budgetService.getBudgetSettings()).thenReturn(mockBudget);

        SummaryDto summary = expenseService.getSummary();

        assertThat(summary.getTotalSpend()).isEqualTo(mockTotalSpend);
        assertThat(summary.getCurrentMonthSpend()).isEqualTo(mockMonthSpend);
        assertThat(summary.getBudgetLimit()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(summary.getRemainingBudget()).isEqualTo(new BigDecimal("500.00"));
        assertThat(summary.getBudgetPercentage()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Get Expenses should call findAll with specification and sort")
    void testGetExpenses() {
        List<Expense> mockExpenses = List.of(sampleExpense);
        when(expenseRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(mockExpenses);

        List<Expense> result = expenseService.getExpenses("Food", "2026-06");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Food");
        verify(expenseRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("Get Category Breakdown should return list of CategorySumDto")
    void testGetCategoryBreakdown() {
        ExpenseRepository.CategorySum mockCategorySum = mock(ExpenseRepository.CategorySum.class);
        when(mockCategorySum.getCategory()).thenReturn("Food");
        when(mockCategorySum.getTotalAmount()).thenReturn(new BigDecimal("120.50"));

        when(expenseRepository.getCategoryBreakdown(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(mockCategorySum));

        List<CategorySumDto> breakdown = expenseService.getCategoryBreakdown("2026-06");

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).getCategory()).isEqualTo("Food");
        assertThat(breakdown.get(0).getTotalAmount()).isEqualTo(new BigDecimal("120.50"));
    }

    @Test
    @DisplayName("Get Monthly Trends should return list of MonthlyTrendDto within limit")
    void testGetMonthlyTrends() {
        when(expenseRepository.sumExpensesBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("500.00"), new BigDecimal("600.00"));

        List<MonthlyTrendDto> trends = expenseService.getMonthlyTrends(2);

        assertThat(trends).hasSize(2);
        assertThat(trends.get(0).getTotalAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(trends.get(1).getTotalAmount()).isEqualTo(new BigDecimal("600.00"));
        verify(expenseRepository, times(2)).sumExpensesBetween(any(LocalDate.class), any(LocalDate.class));
    }
}
