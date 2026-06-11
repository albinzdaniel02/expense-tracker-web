# Implementation Plan - Expense Tracker

This document provides a detailed, step-by-step roadmap for building the self-hosted **Expense Tracker** application. It aligns all phases of construction with the requirements, architecture, API contracts, and wireframes.

---

## Phase 1: Database & Dev Environment Configuration

In this phase, we establish the local development runtime environment using Docker Compose and bootstrap the Maven build project with its dependencies and database configurations.

### 1.1. Docker Compose PostgreSQL Setup
Create `docker-compose.yml` in the project root directory. This containerized instance ensures zero overhead on the developer's local OS.

```yaml
version: '3.8'

services:
  postgres-db:
    image: postgres:16-alpine
    container_name: expense-tracker-db
    environment:
      POSTGRES_DB: expensetracker
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

*Action*: Run `docker-compose up -d` to pull the PostgreSQL 16 image and start the database.

### 1.2. Maven Dependencies Setup (`pom.xml`)
Initialize the project structure with Java 21 and Spring Boot 3.x. Include the following core dependencies:

```xml
<dependencies>
    <!-- Web Support (REST API & static resource hosting) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Data Persistence -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok (compile-time code generation) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Validation API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

### 1.3. Application Properties Binding (`src/main/resources/application.properties`)
Configure JDBC connections and Hibernate behavior:

```properties
# Server Port Configuration
server.port=8080

# Database Bindings
spring.datasource.url=jdbc:postgresql://localhost:5432/expensetracker
spring.datasource.username=admin
spring.datasource.password=admin
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Auto DDL Update & Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## Phase 2: Domain Entities & Repositories

Map relational tables to Java models using JPA. Implement UUID keys for both entities, including standard configurations.

### 2.1. JPA Entities Design

#### `Expense.java` (`com.expensetracker.entity.Expense`)
```java
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
```

#### `BudgetSettings.java` (`com.expensetracker.entity.BudgetSettings`)
```java
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
```

### 2.2. JPA Repositories & Custom Query Methods

#### `ExpenseRepository.java` (`com.expensetracker.repository.ExpenseRepository`)
```java
package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    // Retrieve Overall Total Spent
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllExpenses();

    // Retrieve Spent for Specific Date Range
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumExpensesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Category Aggregations DTO projection mapping
    @Query("SELECT e.category AS category, SUM(e.amount) AS totalAmount " +
           "FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category " +
           "ORDER BY totalAmount DESC")
    List<CategorySum> getCategoryBreakdown(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Projection Interface for Category Aggregations
    interface CategorySum {
        String getCategory();
        BigDecimal getTotalAmount();
    }
}
```

#### `BudgetSettingsRepository.java` (`com.expensetracker.repository.BudgetSettingsRepository`)
```java
package com.expensetracker.repository;

import com.expensetracker.entity.BudgetSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BudgetSettingsRepository extends JpaRepository<BudgetSettings, UUID> {
}
```

---

## Phase 3: Core Service & Business Logic

Inject Lombok features (`@RequiredArgsConstructor`) to manage constructor injections. Implement CRUD operations, statistics computations, and the budget seeding behavior.

### 3.1. DTO Classes

Create a package `com.expensetracker.dto` containing standard payloads:
- `ExpenseRequest.java` and `BudgetSettingsRequest.java` for updates.
- `SummaryDto.java` containing the metrics:
  ```java
  package com.expensetracker.dto;
  import lombok.*;
  import java.math.BigDecimal;
  @Data
  @Builder
  public class SummaryDto {
      private BigDecimal totalSpend;
      private BigDecimal currentMonthSpend;
      private BigDecimal remainingBudget;
      private BigDecimal budgetLimit;
      private BigDecimal budgetPercentage;
  }
  ```
- `CategorySumDto.java`:
  ```java
  package com.expensetracker.dto;
  import lombok.*;
  import java.math.BigDecimal;
  @Data
  @AllArgsConstructor
  public class CategorySumDto {
      private String category;
      private BigDecimal totalAmount;
  }
  ```
- `MonthlyTrendDto.java`:
  ```java
  package com.expensetracker.dto;
  import lombok.*;
  import java.math.BigDecimal;
  @Data
  @AllArgsConstructor
  public class MonthlyTrendDto {
      private String monthYear; // Format: YYYY-MM
      private BigDecimal totalAmount;
  }
  ```

### 3.2. Budget Seeding Service (`com.expensetracker.service.BudgetService`)
Manage target configuration and data seeding for the singleton configuration record.

```java
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
```

### 3.3. Expense Operations Service (`com.expensetracker.service.ExpenseService`)
Implement business calculations. Maintain dates based on current system timezone calculations.

```java
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
```

---

## Phase 4: REST Controller Layer

Define clean endpoints handling request routing, parameter validation, JSON response packaging, and API error routing.

### 4.1. Expense Controller (`com.expensetracker.controller.ExpenseController`)
```java
package com.expensetracker.controller;

import com.expensetracker.entity.Expense;
import com.expensetracker.dto.*;
import com.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String monthYear) {
        return ResponseEntity.ok(expenseService.getExpenses(category, monthYear));
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody Expense expense) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable UUID id, 
            @Valid @RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<SummaryDto> getSummary() {
        return ResponseEntity.ok(expenseService.getSummary());
    }

    @GetMapping("/stats/category-breakdown")
    public ResponseEntity<List<CategorySumDto>> getCategoryBreakdown(
            @RequestParam(required = false) String monthYear) {
        return ResponseEntity.ok(expenseService.getCategoryBreakdown(monthYear));
    }

    @GetMapping("/stats/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDto>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(expenseService.getMonthlyTrends(limit));
    }
}
```

### 4.2. Budget Settings Controller (`com.expensetracker.controller.BudgetController`)
```java
package com.expensetracker.controller;

import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (!body.containsKey("monthlyLimit")) {
            throw new IllegalArgumentException("Field 'monthlyLimit' is required");
        }
        double monthlyLimitDouble = Double.parseDouble(body.get("monthlyLimit").toString());
        java.math.BigDecimal limit = java.math.BigDecimal.valueOf(monthlyLimitDouble);
        return ResponseEntity.ok(budgetService.updateBudgetSettings(limit));
    }
}
```

### 4.3. Exception Mapping Advisor (`com.expensetracker.config.GlobalExceptionHandler`)
Ensures standard error objects are generated for validation anomalies and database query issues.

```java
package com.expensetracker.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, WebRequest request) {
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException valEx) {
            message = valEx.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage(), request);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, status);
    }
}
```

---

## Phase 5: Frontend Structure & Styling

Configure the presentation layer within Spring Boot's resource mapping: `src/main/resources/static/`.

### 5.1. HTML Structure Layout (`src/main/resources/static/index.html`)
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Expense Tracker - Premium Personal Finance</title>
    <!-- Outfit Font -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <!-- Chart.js via CDN -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div class="app-container">
        <!-- Sticky Navigation Header -->
        <header class="app-header glass-panel">
            <div class="logo-area">
                <span class="logo-gradient">ExpenseTracker</span>
            </div>
            <nav class="nav-links">
                <a href="#" class="nav-item active" data-target="dashboard-view">Dashboard</a>
                <a href="#" class="nav-item" data-target="expenses-view">Expenses</a>
                <a href="#" class="nav-item" data-target="settings-view">Settings</a>
            </nav>
            <button id="theme-toggle-btn" class="theme-toggle-btn" aria-label="Toggle Theme">
                <!-- SVG Icon will render dynamically -->
                <span class="icon"></span>
            </button>
        </header>

        <!-- Warning Notification Banner (Budget Overflow Alert) -->
        <div id="global-alert-banner" class="alert-banner hidden">
            <p><strong>Warning:</strong> You have exceeded your configured monthly budget limit!</p>
        </div>

        <!-- Main Viewports -->
        <main class="main-viewport">
            
            <!-- VIEW 1: DASHBOARD -->
            <section id="dashboard-view" class="view-section">
                <div class="view-header">
                    <h1>Financial Dashboard</h1>
                </div>

                <!-- KPI Metric Cards Grid -->
                <div class="kpi-grid">
                    <div class="kpi-card glass-panel">
                        <h3>Total Spent</h3>
                        <p class="metric-val" id="total-spent-val">$0.00</p>
                        <span class="metric-sub">All-time overall</span>
                    </div>
                    <div class="kpi-card glass-panel">
                        <h3>Current Month Spent</h3>
                        <p class="metric-val" id="month-spent-val">$0.00</p>
                        <span class="metric-sub" id="current-month-lbl">Month</span>
                    </div>
                    <div class="kpi-card glass-panel">
                        <h3>Remaining Budget</h3>
                        <p class="metric-val" id="remaining-budget-val">$0.00</p>
                        <span class="metric-sub" id="budget-month-lbl">Month target</span>
                    </div>
                </div>

                <!-- Budget Consumption Progress Bar -->
                <div class="budget-progress-container glass-panel">
                    <div class="progress-info">
                        <span class="progress-lbl">Budget Consumption</span>
                        <span class="progress-percentage" id="progress-percent-text">0.00% Spent</span>
                    </div>
                    <div class="progress-bar-track">
                        <div id="progress-bar-fill" class="progress-bar-fill bg-safe" style="width: 0%;"></div>
                    </div>
                    <p class="progress-sub-text" id="progress-desc-text">$0.00 spent of $0.00 Limit</p>
                </div>

                <!-- Analytic Charts Grid -->
                <div class="charts-grid">
                    <div class="chart-card glass-panel">
                        <h3>Category Breakdown</h3>
                        <div class="canvas-container">
                            <canvas id="categoryChart"></canvas>
                        </div>
                    </div>
                    <div class="chart-card glass-panel">
                        <h3>Spending Trends (6 Months)</h3>
                        <div class="canvas-container">
                            <canvas id="trendChart"></canvas>
                        </div>
                    </div>
                </div>
            </section>

            <!-- VIEW 2: EXPENSES -->
            <section id="expenses-view" class="view-section hidden">
                <div class="view-header">
                    <h1>Transaction Records</h1>
                    <button class="btn-primary" id="open-add-modal-btn">+ Add Expense</button>
                </div>

                <!-- Filter Panel -->
                <div class="filter-panel glass-panel">
                    <div class="filter-group">
                        <label for="filter-month">Period</label>
                        <input type="month" id="filter-month">
                    </div>
                    <div class="filter-group">
                        <label for="filter-category">Category</label>
                        <select id="filter-category">
                            <option value="">All Categories</option>
                            <option value="Food">Food</option>
                            <option value="Transport">Transport</option>
                            <option value="Utilities">Utilities</option>
                            <option value="Entertainment">Entertainment</option>
                            <option value="Shopping">Shopping</option>
                            <option value="Others">Others</option>
                        </select>
                    </div>
                    <button class="btn-secondary" id="clear-filters-btn">Reset</button>
                </div>

                <!-- Tabular Log -->
                <div class="table-container glass-panel">
                    <table class="expense-table" id="expense-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Category</th>
                                <th>Amount</th>
                                <th class="actions-col">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="expense-table-body">
                            <!-- Populated Dynamically -->
                        </tbody>
                    </table>
                    <div id="no-records-msg" class="no-records hidden">No transactions recorded for the selected parameters.</div>
                </div>
            </section>

            <!-- VIEW 3: SETTINGS -->
            <section id="settings-view" class="view-section hidden">
                <div class="view-header">
                    <h1>Configuration Settings</h1>
                </div>

                <div class="settings-card glass-panel">
                    <h3>Monthly Budget Settings</h3>
                    <p class="settings-desc">Specify your target monthly limit. This limit updates your progress markers immediately.</p>
                    <form id="settings-form">
                        <div class="form-group">
                            <label for="budget-limit-input">Budget Limit ($)</label>
                            <input type="number" id="budget-limit-input" step="0.01" min="0.01" required>
                        </div>
                        <button type="submit" class="btn-primary">Save Config</button>
                    </form>
                </div>
            </section>
        </main>

        <!-- ADD/EDIT MODAL BACKDROP OVERLAY -->
        <div id="expense-modal" class="modal-overlay hidden">
            <div class="modal-card glass-panel">
                <div class="modal-header">
                    <h2 id="modal-title">Add Expense</h2>
                    <button id="close-modal-btn" class="close-btn">&times;</button>
                </div>
                <form id="expense-form">
                    <input type="hidden" id="expense-id-input">
                    
                    <div class="form-group">
                        <label for="amount-input">Amount ($)*</label>
                        <input type="number" id="amount-input" step="0.01" min="0.01" required placeholder="0.00">
                    </div>

                    <div class="form-group">
                        <label for="category-input">Category*</label>
                        <select id="category-input" required>
                            <option value="" disabled selected>Select category</option>
                            <option value="Food">Food</option>
                            <option value="Transport">Transport</option>
                            <option value="Utilities">Utilities</option>
                            <option value="Entertainment">Entertainment</option>
                            <option value="Shopping">Shopping</option>
                            <option value="Others">Others</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="date-input">Date*</label>
                        <input type="date" id="date-input" required>
                    </div>

                    <div class="modal-actions">
                        <button type="button" class="btn-secondary" id="cancel-modal-btn">Cancel</button>
                        <button type="submit" class="btn-primary" id="save-expense-btn">Save Expense</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="js/app.js"></script>
</body>
</html>
```

### 5.2. Glassmorphism Design Styling Tokens (`src/main/resources/static/css/style.css`)
```css
/* Styling Token Declarations */
:root {
    --font-sans: 'Outfit', 'Inter', system-ui, -apple-system, sans-serif;
    
    --space-2xs: 4px;
    --space-xs: 8px;
    --space-sm: 12px;
    --space-md: 16px;
    --space-lg: 24px;
    --space-xl: 32px;

    /* Theme: Light Mode Base */
    --bg-main: radial-gradient(circle at top right, #f1f5f9, #cbd5e1);
    --glass-bg: rgba(255, 255, 255, 0.45);
    --glass-border: rgba(255, 255, 255, 0.35);
    --glass-shadow: 0 8px 32px 0 rgba(148, 163, 184, 0.15);
    
    --text-main: #0f172a;
    --text-muted: #475569;
    --primary-accent: #4f46e5;
    --primary-hover: #4338ca;
    --border-color: rgba(255, 255, 255, 0.4);

    /* Alert Variables */
    --alert-safe: #10b981;
    --alert-warning: #f59e0b;
    --alert-critical: #ef4444;
}

[data-theme="dark"] {
    /* Theme: Dark Mode Overrides */
    --bg-main: radial-gradient(circle at top right, #0f172a, #020617);
    --glass-bg: rgba(15, 23, 42, 0.65);
    --glass-border: rgba(255, 255, 255, 0.08);
    --glass-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.4);
    
    --text-main: #f8fafc;
    --text-muted: #94a3b8;
    --primary-accent: #818cf8;
    --primary-hover: #6366f1;
}

* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: var(--font-sans);
    background: var(--bg-main);
    color: var(--text-main);
    min-height: 100vh;
    padding: var(--space-xl) var(--space-md);
    transition: background 0.3s ease, color 0.3s ease;
}

.app-container {
    max-width: 1200px;
    margin: 0 auto;
}

/* Glassmorphic Panel Foundation */
.glass-panel {
    background: var(--glass-bg);
    backdrop-filter: blur(12px) saturate(180%);
    -webkit-backdrop-filter: blur(12px) saturate(180%);
    border: 1px solid var(--glass-border);
    box-shadow: var(--glass-shadow);
    border-radius: 16px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-panel:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.15);
}

/* Header Styling */
.app-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-md) var(--space-lg);
    margin-bottom: var(--space-xl);
}

.logo-area {
    font-size: 1.5rem;
    font-weight: 700;
}

.logo-gradient {
    background: linear-gradient(135deg, var(--primary-accent), #ec4899);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.nav-links {
    display: flex;
    gap: var(--space-md);
}

.nav-item {
    text-decoration: none;
    color: var(--text-muted);
    font-weight: 500;
    padding: var(--space-xs) var(--space-md);
    border-radius: 8px;
    transition: all 0.2s ease;
}

.nav-item:hover, .nav-item.active {
    color: var(--text-main);
    background: rgba(255, 255, 255, 0.2);
}

/* Theme Toggle Button styling */
.theme-toggle-btn {
    background: transparent;
    border: 1px solid var(--glass-border);
    padding: var(--space-xs);
    border-radius: 50%;
    cursor: pointer;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-main);
    transition: background 0.2s ease;
}

.theme-toggle-btn:hover {
    background: rgba(255, 255, 255, 0.2);
}

/* Grid & Layout Adjustments */
.view-section {
    transition: opacity 0.2s ease-in-out;
}

.view-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-lg);
}

/* KPI Cards Layout */
.kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: var(--space-lg);
    margin-bottom: var(--space-xl);
}

.kpi-card {
    padding: var(--space-lg);
}

.kpi-card h3 {
    font-size: 0.875rem;
    color: var(--text-muted);
    text-transform: uppercase;
    margin-bottom: var(--space-xs);
}

.metric-val {
    font-size: 2.25rem;
    font-weight: 700;
    color: var(--text-main);
    margin-bottom: var(--space-2xs);
}

.metric-sub {
    font-size: 0.75rem;
    color: var(--text-muted);
}

/* Budget Progress Bar Styling */
.budget-progress-container {
    padding: var(--space-lg);
    margin-bottom: var(--space-xl);
}

.progress-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-xs);
}

.progress-lbl {
    font-weight: 600;
}

.progress-percentage {
    font-weight: 700;
}

.progress-bar-track {
    width: 100%;
    height: 12px;
    background: rgba(0, 0, 0, 0.1);
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: var(--space-xs);
}

.progress-bar-fill {
    height: 100%;
    border-radius: 6px;
    transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.3s ease;
}

.bg-safe {
    background: linear-gradient(90deg, #3b82f6, var(--alert-safe));
}

.bg-warning {
    background: var(--alert-warning);
}

.bg-critical {
    background: var(--alert-critical);
}

.progress-sub-text {
    font-size: 0.875rem;
    color: var(--text-muted);
}

/* Charts Grid */
.charts-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
    gap: var(--space-lg);
}

.chart-card {
    padding: var(--space-lg);
}

.chart-card h3 {
    margin-bottom: var(--space-lg);
}

.canvas-container {
    position: relative;
    height: 300px;
    width: 100%;
}

/* Filter Panel Styling */
.filter-panel {
    display: flex;
    align-items: flex-end;
    gap: var(--space-lg);
    padding: var(--space-lg);
    margin-bottom: var(--space-lg);
}

.filter-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xs);
    flex-grow: 1;
}

.filter-group label {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-muted);
}

.filter-group select, .filter-group input {
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid var(--glass-border);
    padding: var(--space-xs) var(--space-sm);
    border-radius: 8px;
    color: var(--text-main);
    font-family: var(--font-sans);
    outline: none;
    transition: border 0.2s ease, box-shadow 0.2s ease;
}

.filter-group select:focus, .filter-group input:focus {
    border-color: var(--primary-accent);
    box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.25);
}

/* Primary/Secondary Buttons */
.btn-primary {
    background: linear-gradient(135deg, var(--primary-accent), var(--primary-hover));
    border: none;
    color: #ffffff;
    padding: var(--space-sm) var(--space-lg);
    border-radius: 8px;
    font-weight: 500;
    cursor: pointer;
    transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), filter 0.2s ease;
}

.btn-primary:hover {
    transform: scale(1.02);
    filter: brightness(1.1);
}

.btn-primary:active {
    transform: scale(0.98);
}

.btn-secondary {
    background: transparent;
    border: 1px solid var(--glass-border);
    color: var(--text-main);
    padding: var(--space-sm) var(--space-lg);
    border-radius: 8px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
}

.btn-secondary:hover {
    background: rgba(255, 255, 255, 0.15);
}

/* Tables styling */
.table-container {
    overflow-x: auto;
    padding: var(--space-md);
}

.expense-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
}

.expense-table th, .expense-table td {
    padding: var(--space-md);
    border-bottom: 1px solid var(--glass-border);
}

.expense-table th {
    color: var(--text-muted);
    font-weight: 600;
}

.actions-col {
    text-align: right;
}

.action-btn {
    background: transparent;
    border: none;
    color: var(--text-muted);
    cursor: pointer;
    padding: var(--space-2xs);
    border-radius: 4px;
    transition: all 0.2s ease;
    margin-left: var(--space-2xs);
}

.action-btn:hover {
    color: var(--text-main);
    background: rgba(255, 255, 255, 0.2);
}

.action-btn.delete:hover {
    color: var(--alert-critical);
}

/* Modals styling */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(15, 23, 42, 0.6);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    transition: opacity 0.3s ease;
}

.modal-card {
    width: 100%;
    max-width: 500px;
    padding: var(--space-lg);
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-lg);
}

.close-btn {
    background: transparent;
    border: none;
    font-size: 1.5rem;
    color: var(--text-muted);
    cursor: pointer;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xs);
    margin-bottom: var(--space-md);
}

.form-group label {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-muted);
}

.form-group input, .form-group select {
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid var(--glass-border);
    padding: var(--space-sm);
    border-radius: 8px;
    color: var(--text-main);
    font-family: var(--font-sans);
    outline: none;
}

.form-group input:focus, .form-group select:focus {
    border-color: var(--primary-accent);
    box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.25);
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-md);
    margin-top: var(--space-lg);
}

/* Helper Utilities */
.hidden {
    display: none !important;
}

.alert-banner {
    background: var(--alert-critical);
    color: white;
    padding: var(--space-md);
    border-radius: 8px;
    margin-bottom: var(--space-lg);
    text-align: center;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.no-records {
    padding: var(--space-xl);
    text-align: center;
    color: var(--text-muted);
}

/* Animations */
@keyframes fadeOut {
    from { opacity: 1; transform: scale(1); }
    to { opacity: 0; transform: scale(0.9); }
}

.fade-out {
    animation: fadeOut 0.3s ease forwards;
}
```

---

## Phase 6: Frontend SPA Routing & Event Listeners

Create `src/main/resources/static/js/app.js`. Handle click bindings for view switching, layout configurations, and form submissions.

### 6.1. SPA Router State & Navigation
```javascript
// Global Application State Cache
const state = {
    currentView: 'dashboard-view',
    expenses: [],
    budgetLimit: 1000.00,
    filters: {
        category: '',
        monthYear: new Date().toISOString().substring(0, 7) // Current month (YYYY-MM)
    },
    theme: 'light'
};

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    setupEventListeners();
    navigateTo('dashboard-view');
});

// Central Navigation Controller
function navigateTo(viewId) {
    state.currentView = viewId;
    
    // Toggle Section visibility
    document.querySelectorAll('.view-section').forEach(section => {
        if (section.id === viewId) {
            section.classList.remove('hidden');
        } else {
            section.classList.add('hidden');
        }
    });

    // Update Nav Item highlight classes
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('data-target') === viewId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Fetch view-specific dataset on navigation
    if (viewId === 'dashboard-view') {
        refreshDashboard();
    } else if (viewId === 'expenses-view') {
        refreshExpensesLog();
    } else if (viewId === 'settings-view') {
        loadSettingsForm();
    }
}
```

### 6.2. Theme Toggling Logic
Configure dark mode swapping. Use SVGs to toggle theme symbols dynamically.

```javascript
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
}

function setTheme(theme) {
    state.theme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
    
    const themeBtn = document.getElementById('theme-toggle-btn');
    if (theme === 'dark') {
        themeBtn.innerHTML = `
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 3a9 9 0 1 0 9 9 9.01 9.01 0 0 0-9-9zm0 16a7 7 0 1 1 7-7 7.008 7.008 0 0 1-7 7z"/>
            </svg>`;
    } else {
        themeBtn.innerHTML = `
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 18a6 6 0 1 1 6-6 6.008 6.008 0 0 1-6 6zm0-10a4 4 0 1 0 4 4 4.005 4.005 0 0 0-4-4zM11 1h2v3h-2zm0 19h2v3h-2zm10-9h3v2h-3zm-17 0h3v2H4zm13.657-6.243l1.414 1.414-2.121 2.121-1.414-1.414zm-12.02 12.02l1.414 1.414-2.121 2.121-1.414-1.414zm12.02 0l2.121 2.121-1.414 1.414-2.121-2.121zm-13.434-13.434l2.121 2.121-1.414 1.414-2.121-2.121z"/>
            </svg>`;
    }
}

function toggleTheme() {
    setTheme(state.theme === 'light' ? 'dark' : 'light');
}
```

### 6.3. Event Listeners Setup
```javascript
function setupEventListeners() {
    // Navigation link clicks
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(item.getAttribute('data-target'));
        });
    });

    // Theme Toggle
    document.getElementById('theme-toggle-btn').addEventListener('click', toggleTheme);

    // Modal Control triggers
    document.getElementById('open-add-modal-btn').addEventListener('click', () => openExpenseModal());
    document.getElementById('close-modal-btn').addEventListener('click', closeExpenseModal);
    document.getElementById('cancel-modal-btn').addEventListener('click', closeExpenseModal);
    
    // Expense form submission
    document.getElementById('expense-form').addEventListener('submit', handleExpenseSubmit);

    // Filter Listeners
    document.getElementById('filter-month').addEventListener('input', (e) => {
        state.filters.monthYear = e.target.value;
        refreshExpensesLog();
    });
    
    document.getElementById('filter-category').addEventListener('change', (e) => {
        state.filters.category = e.target.value;
        refreshExpensesLog();
    });
    
    document.getElementById('clear-filters-btn').addEventListener('click', () => {
        document.getElementById('filter-category').value = "";
        document.getElementById('filter-month').value = new Date().toISOString().substring(0, 7);
        state.filters.category = "";
        state.filters.monthYear = new Date().toISOString().substring(0, 7);
        refreshExpensesLog();
    });

    // Settings form submission
    document.getElementById('settings-form').addEventListener('submit', handleSettingsSubmit);
}
```

---

## Phase 7: API Integrations & Chart.js Integration

Assemble asynchronous calls using standard native `fetch()` calls. Build UI manipulation bindings and chart updates.

### 7.1. HTTP API Helper Functions
```javascript
// API Core Client Calls
async function apiFetch(endpoint, options = {}) {
    const defaults = {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };
    
    const response = await fetch(endpoint, { ...defaults, ...options });
    
    if (response.status === 204) return null;
    
    if (!response.ok) {
        const errPayload = await response.json();
        throw new Error(errPayload.message || 'API request failed');
    }
    
    return response.json();
}
```

### 7.2. DOM Bindings and Actions

```javascript
// Fetch and Render log lists
async function refreshExpensesLog() {
    try {
        const { category, monthYear } = state.filters;
        let queryParams = [];
        if (category) queryParams.push(`category=${encodeURIComponent(category)}`);
        if (monthYear) queryParams.push(`monthYear=${encodeURIComponent(monthYear)}`);
        
        const queryString = queryParams.length ? `?${queryParams.join('&')}` : '';
        const expenses = await apiFetch(`/api/expenses${queryString}`);
        state.expenses = expenses;

        renderExpenseTable(expenses);
    } catch (error) {
        alert("Failed to fetch expenses: " + error.message);
    }
}

// Render dynamic rows with UUID bindings
function renderExpenseTable(expenses) {
    const tbody = document.getElementById('expense-table-body');
    const msg = document.getElementById('no-records-msg');
    tbody.innerHTML = '';
    
    if (expenses.length === 0) {
        msg.classList.remove('hidden');
        return;
    }
    msg.classList.add('hidden');

    expenses.forEach(exp => {
        const tr = document.createElement('tr');
        // Bind UUID directly to the DOM node
        tr.setAttribute('data-id', exp.id);
        
        tr.innerHTML = `
            <td>${exp.expenseDate}</td>
            <td><span class="category-badge">${exp.category}</span></td>
            <td>$${exp.amount.toFixed(2)}</td>
            <td class="actions-col">
                <button class="action-btn edit" onclick="editExpense('${exp.id}')">Edit</button>
                <button class="action-btn delete" onclick="deleteExpenseConfirm('${exp.id}')">Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}
```

### 7.3. Modal Forms, Edit, and Delete Lifecycle
```javascript
function openExpenseModal(expense = null) {
    const modal = document.getElementById('expense-modal');
    const title = document.getElementById('modal-title');
    
    document.getElementById('expense-id-input').value = expense ? expense.id : '';
    document.getElementById('amount-input').value = expense ? expense.amount : '';
    document.getElementById('category-input').value = expense ? expense.category : '';
    document.getElementById('date-input').value = expense ? expense.expenseDate : new Date().toISOString().substring(0, 10);
    
    title.innerText = expense ? 'Edit Expense' : 'Add Expense';
    modal.classList.remove('hidden');
}

function closeExpenseModal() {
    document.getElementById('expense-modal').classList.add('hidden');
}

function editExpense(id) {
    const expense = state.expenses.find(e => e.id === id);
    if (expense) openExpenseModal(expense);
}

async function deleteExpenseConfirm(id) {
    if (confirm("Are you sure you want to delete this expense record?")) {
        try {
            await apiFetch(`/api/expenses/${id}`, { method: 'DELETE' });
            
            // Remove matching row in the DOM using a simple fade-out transition
            const row = document.querySelector(`tr[data-id="${id}"]`);
            if (row) {
                row.classList.add('fade-out');
                setTimeout(() => {
                    row.remove();
                    refreshExpensesLog();
                    refreshDashboard();
                }, 300);
            }
        } catch (error) {
            alert("Failed to delete expense: " + error.message);
        }
    }
}

async function handleExpenseSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('expense-id-input').value;
    const payload = {
        amount: parseFloat(document.getElementById('amount-input').value),
        category: document.getElementById('category-input').value,
        expenseDate: document.getElementById('date-input').value
    };

    try {
        if (id) {
            // Update mode
            await apiFetch(`/api/expenses/${id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
        } else {
            // Create mode
            await apiFetch('/api/expenses', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        }
        closeExpenseModal();
        refreshExpensesLog();
        refreshDashboard();
    } catch (error) {
        alert("Failed to save transaction: " + error.message);
    }
}
```

### 7.4. Dashboard Charts Lifecycle Management

Store chart instances globally. **Important**: Explicitly call `.destroy()` on previous configurations on each refresh to clear canvas state.

```javascript
let categoryChartInstance = null;
let trendChartInstance = null;

async function refreshDashboard() {
    try {
        // Fetch summary metrics DTO
        const summary = await apiFetch('/api/expenses/stats/summary');
        
        // Populate Metric Cards
        document.getElementById('total-spent-val').innerText = `$${summary.totalSpend.toFixed(2)}`;
        document.getElementById('month-spent-val').innerText = `$${summary.currentMonthSpend.toFixed(2)}`;
        document.getElementById('remaining-budget-val').innerText = `$${summary.remainingBudget.toFixed(2)}`;
        
        // Dynamically update labels
        const currentMonthString = new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
        document.getElementById('current-month-lbl').innerText = `${currentMonthString} Total`;
        document.getElementById('budget-month-lbl').innerText = `${currentMonthString} Target`;

        // Update progress bar calculations
        const percentage = summary.budgetPercentage;
        const barFill = document.getElementById('progress-bar-fill');
        barFill.style.width = `${Math.min(percentage, 100)}%`;
        document.getElementById('progress-percent-text').innerText = `${percentage.toFixed(2)}% Spent`;
        document.getElementById('progress-desc-text').innerText = `$${summary.currentMonthSpend.toFixed(2)} spent of $${summary.budgetLimit.toFixed(2)} Limit`;

        // Update Alert threshold coloring
        barFill.classList.remove('bg-safe', 'bg-warning', 'bg-critical');
        const alertBanner = document.getElementById('global-alert-banner');
        
        if (percentage < 80.00) {
            barFill.classList.add('bg-safe');
            alertBanner.classList.add('hidden');
        } else if (percentage < 100.00) {
            barFill.classList.add('bg-warning');
            alertBanner.classList.add('hidden');
        } else {
            barFill.classList.add('bg-critical');
            alertBanner.classList.remove('hidden');
        }

        // Load analytical charts
        await updateCategoryChart();
        await updateTrendChart();
        
    } catch (error) {
        console.error("Dashboard refresh error: ", error);
    }
}

// Chart.js Category Breakdown rendering
async function updateCategoryChart() {
    const data = await apiFetch('/api/expenses/stats/category-breakdown');
    const labels = data.map(item => item.category);
    const amounts = data.map(item => item.totalAmount);

    if (categoryChartInstance) {
        categoryChartInstance.destroy();
    }

    const ctx = document.getElementById('categoryChart').getContext('2d');
    categoryChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: amounts,
                backgroundColor: ['#4f46e5', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1', '#64748b']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: getComputedStyle(document.documentElement).getPropertyValue('--text-main').trim() }
                }
            }
        }
    });
}

// Chart.js Trends rendering
async function updateTrendChart() {
    const data = await apiFetch('/api/expenses/stats/monthly-trends');
    const labels = data.map(item => item.monthYear);
    const amounts = data.map(item => item.totalAmount);

    if (trendChartInstance) {
        trendChartInstance.destroy();
    }

    const ctx = document.getElementById('trendChart').getContext('2d');
    trendChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Monthly Spending',
                data: amounts,
                backgroundColor: 'rgba(79, 70, 229, 0.6)',
                borderColor: '#4f46e5',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--text-main').trim() }
                },
                x: {
                    ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--text-main').trim() }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}

// Budget configuration forms
async function loadSettingsForm() {
    try {
        const settings = await apiFetch('/api/budget-settings');
        state.budgetLimit = settings.monthlyLimit;
        document.getElementById('budget-limit-input').value = settings.monthlyLimit;
    } catch (error) {
        alert("Failed to load budget configuration: " + error.message);
    }
}

async function handleSettingsSubmit(e) {
    e.preventDefault();
    const newLimit = parseFloat(document.getElementById('budget-limit-input').value);
    
    try {
        await apiFetch('/api/budget-settings', {
            method: 'PUT',
            body: JSON.stringify({ monthlyLimit: newLimit })
        });
        alert("Configuration updated successfully!");
        navigateTo('dashboard-view');
    } catch (error) {
        alert("Failed to update configurations: " + error.message);
    }
}
```

---

## Phase 8: Compilation, Packaging & Running Locally

In this final phase, the system builds into a single self-contained executable file.

### 8.1. Maven compilation build validation
Execute the compiler package script in the project root:

```powershell
mvn clean package
```

This commands executes the following lifecycle actions:
1. Compiles Java source files inside `src/main/java`.
2. Validates package configurations and maps static web code from `src/main/resources/static/` directly into the package bundle.
3. Packages the application class structures and configurations into a single executable JAR located at: `target/expense-tracker-0.0.1-SNAPSHOT.jar`.

### 8.2. Local startup process execution
Run the compiled self-contained artifact:

```powershell
java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar
```

Ensure the PostgreSQL docker container (`expense-tracker-db`) is active beforehand.

### 8.3. App Port & Verification Details
- **Port Mapping**: The application initiates on local port `8080`.
- **System Verification URL**: Open `http://localhost:8080` in a web browser.
- **Initial Verification Checklist**:
  1. Verify the homepage loads index assets (fonts, layouts) styled with glassmorphism parameters.
  2. Navigate to Settings page. Ensure the default budget of `$1000.00` was automatically seeded and renders in the form field.
  3. Verify toggle buttons switch theme contexts (Light/Dark themes) without page reloads.
  4. Submit a transaction entry using the "+ Add Expense" modal form and ensure dashboard metric cards, progress bars, and Chart.js animations refresh immediately.
