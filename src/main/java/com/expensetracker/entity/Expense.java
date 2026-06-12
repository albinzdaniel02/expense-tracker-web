package com.expensetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Expense amount must not be null")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotBlank(message = "Category must not be blank")
    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @NotNull(message = "Expense date must not be null")
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
}
