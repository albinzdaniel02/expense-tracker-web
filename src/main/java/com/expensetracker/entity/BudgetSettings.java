package com.expensetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSettings {

    @Id
    private UUID id; // Will be bound to 00000000-0000-0000-0000-000000000000

    @NotNull(message = "Monthly budget limit must not be null")
    @DecimalMin(value = "0.01", message = "Monthly budget limit must be greater than zero")
    @Column(name = "monthly_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal monthlyLimit;
}
