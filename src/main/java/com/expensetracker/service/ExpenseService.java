package com.expensetracker.service;

import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.entity.Expense;
import com.expensetracker.dto.*;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public List<Expense> getExpenses(String category, String monthYear) {
        Specification<Expense> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            
            if (monthYear != null && !monthYear.trim().isEmpty()) {
                YearMonth ym = YearMonth.parse(monthYear);
                predicates.add(cb.between(root.get("expenseDate"), ym.atDay(1), ym.atEndOfMonth()));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        return expenseRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "expenseDate"));
    }

    @Transactional
    public Expense createExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(UUID id, Expense updatedDetails) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Expense not found with ID: " + id));
        expense.setAmount(updatedDetails.getAmount());
        expense.setCategory(updatedDetails.getCategory());
        expense.setExpenseDate(updatedDetails.getExpenseDate());
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        if (!expenseRepository.existsById(id)) {
            throw new NoSuchElementException("Expense not found with ID: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SummaryDto getSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        BigDecimal totalSpend = expenseRepository.sumAllExpenses();
        BigDecimal currentMonthSpend = expenseRepository.sumExpensesBetween(start, end);
        BudgetSettings budget = budgetService.getBudgetSettings();
        BigDecimal budgetLimit = budget.getMonthlyLimit();

        BigDecimal remainingBudget = budgetLimit.subtract(currentMonthSpend);
        
        BigDecimal budgetPercentage = BigDecimal.ZERO;
        if (budgetLimit.compareTo(BigDecimal.ZERO) > 0) {
            budgetPercentage = currentMonthSpend
                    .multiply(new BigDecimal("100"))
                    .divide(budgetLimit, 2, RoundingMode.HALF_UP);
        }

        return SummaryDto.builder()
                .totalSpend(totalSpend)
                .currentMonthSpend(currentMonthSpend)
                .remainingBudget(remainingBudget)
                .budgetLimit(budgetLimit)
                .budgetPercentage(budgetPercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategorySumDto> getCategoryBreakdown(String monthYear) {
        YearMonth ym = (monthYear != null && !monthYear.trim().isEmpty()) 
                ? YearMonth.parse(monthYear) 
                : YearMonth.now();
        
        return expenseRepository.getCategoryBreakdown(ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .map(proj -> new CategorySumDto(proj.getCategory(), proj.getTotalAmount()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendDto> getMonthlyTrends(int limit) {
        LocalDate today = LocalDate.now();
        List<MonthlyTrendDto> trends = new ArrayList<>();
        
        for (int i = limit - 1; i >= 0; i--) {
            YearMonth targetMonth = YearMonth.from(today).minusMonths(i);
            BigDecimal sum = expenseRepository.sumExpensesBetween(targetMonth.atDay(1), targetMonth.atEndOfMonth());
            String monthString = targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            trends.add(new MonthlyTrendDto(monthString, sum));
        }
        
        return trends;
    }
}
