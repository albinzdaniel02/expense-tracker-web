package com.expensetracker.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpenseValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidExpense() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("10.50"))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).isEmpty();
    }

    @Test
    public void testInvalidAmount_Null() {
        Expense expense = Expense.builder()
                .amount(null)
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Expense amount must not be null");
    }

    @Test
    public void testInvalidAmount_TooLow() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("0.00"))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Expense amount must be greater than zero");
    }

    @Test
    public void testInvalidCategory_Blank() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("10.00"))
                .category("   ")
                .expenseDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category must not be blank");
    }

    @Test
    public void testInvalidCategory_Null() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("10.00"))
                .category(null)
                .expenseDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category must not be blank");
    }

    @Test
    public void testInvalidDate_Null() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("10.00"))
                .category("Food")
                .expenseDate(null)
                .build();

        Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Expense date must not be null");
    }
}
