# Web Expense Tracker - Implementation Tasks

This document details the step-by-step roadmap for building the self-hosted **Expense Tracker** application based on the system requirements, API contracts, technical architecture, implementation plan, and testing plan.

## General Instructions

> [!IMPORTANT]
> - **Branching Strategy**: All tasks must be completed on a branch of its own named after the task. Example: `P0-1`, `P2-3`, etc.
> - **Commit Strategy**: Only atomic commits must be made, with clear and descriptive commit messages matching the scope of the task.
> - **CI Validation**: The CI pipeline (created in `P0-2`) will run automated validation checks on all pushed branches.

---

## Phase 0: Development Environment & CI Setup (`P0`)
- [x] **P0-1**: Initialize the project directory structure, configure `pom.xml` with dependencies (Spring Web, Spring Data JPA, Lombok, Validation, PostgreSQL Driver), configure database parameters in `application.properties`, and define `docker-compose.yml` for a containerized PostgreSQL 16 database.
  - **Branch**: `P0-1`
- [x] **P0-2**: Set up GitHub Actions CI workflow (`.github/workflows/ci.yml`) to automatically compile the application, run code checks, and execute tests on every push/pull request.
  - **Branch**: `P0-2`

### Phase 0 Exit Checks
- [x] Docker container for PostgreSQL (`expense-tracker-db`) runs locally and accepts connections on port 5432.
- [x] The Spring Boot backend compiles successfully with no Maven/dependency issues (`mvn compile`).
- [x] GitHub Actions workflow configuration is valid and successfully triggers on push.

---

## Phase 1: Domain Entities & Repositories (`P1`)
- [x] **P1-1**: Create `Expense.java` entity class (`com.expensetracker.entity.Expense`) representing transaction records with JSR-380 validation.
  - **Branch**: `P1-1`
- [x] **P1-2**: Create `BudgetSettings.java` entity class (`com.expensetracker.entity.BudgetSettings`) to represent the monthly budget limit configuration.
  - **Branch**: `P1-2`
- [x] **P1-3**: Implement `ExpenseRepository.java` (`com.expensetracker.repository.ExpenseRepository`) interface with custom JPQL queries for overall spend, month-specific range spend, and category breakdown aggregations.
  - **Branch**: `P1-3`
- [ ] **P1-4**: Implement `BudgetSettingsRepository.java` (`com.expensetracker.repository.BudgetSettingsRepository`) for saving budget limits.
  - **Branch**: `P1-4`

### Phase 1 Exit Checks
- [ ] Database schema is successfully auto-generated in PostgreSQL on application startup (`spring.jpa.hibernate.ddl-auto=update`).
- [ ] Both repository layers compile cleanly and connect database fields to JPA entities.

---

## Phase 2: Core Service & Business Logic (`P2`)
- [ ] **P2-1**: Create required Data Transfer Objects (DTOs) under package `com.expensetracker.dto` (`SummaryDto`, `CategorySumDto`, `MonthlyTrendDto`).
  - **Branch**: `P2-1`
- [ ] **P2-2**: Create `BudgetService.java` (`com.expensetracker.service.BudgetService`) handling retrieval, default seeding of the singleton budget limit (ID: `00000000-0000-0000-0000-000000000000`), and updating budget configurations.
  - **Branch**: `P2-2`
- [ ] **P2-3**: Create `ExpenseService.java` (`com.expensetracker.service.ExpenseService`) containing full CRUD operations, specification-based filtering (by category/date), and aggregation computations (current month spend, trends).
  - **Branch**: `P2-3`
- [ ] **P2-4**: Implement comprehensive backend unit tests (`ExpenseServiceTest.java` and `BudgetServiceTest.java`) matching the requirements in `testing-plan.md`.
  - **Branch**: `P2-4`

### Phase 2 Exit Checks
- [ ] Unit tests pass successfully via Maven (`mvn test`).
- [ ] Business logic code coverage validation checks pass without errors.

---

## Phase 3: REST Controller Layer (`P3`)
- [ ] **P3-1**: Implement `ExpenseController.java` (`com.expensetracker.controller.ExpenseController`) with endpoints for retrieving/filtering expenses, creating new expenses, updating entries, and deleting records.
  - **Branch**: `P3-1`
- [ ] **P3-2**: Implement `BudgetController.java` (`com.expensetracker.controller.BudgetController`) with endpoints for retrieving and updating budget limits.
  - **Branch**: `P3-2`
- [ ] **P3-3**: Create a global exception handler (`com.expensetracker.exception.GlobalExceptionHandler`) to translate validation failures and not-found exceptions into structured JSON error payloads.
  - **Branch**: `P3-3`
- [ ] **P3-4**: Implement controller integration tests (`ExpenseControllerTest.java` and `BudgetControllerTest.java`) utilizing MockMvc matching the requirements in `testing-plan.md`.
  - **Branch**: `P3-4`

### Phase 3 Exit Checks
- [ ] REST API endpoints return response formats and status codes aligned with `api-contracts.md`.
- [ ] Controller integration tests run and pass successfully via Maven (`mvn test`).

---

## Phase 4: Frontend Structure & Styling (`P4`)
- [ ] **P4-1**: Create the main HTML file (`src/main/resources/static/index.html`) using semantic tags, containing layout divisions for dashboard, expense logs, settings, and CRUD modals.
  - **Branch**: `P4-1`
- [ ] **P4-2**: Implement responsive vanilla CSS stylesheet (`src/main/resources/static/css/style.css`) using custom properties for theme styling, dark/light themes, and glassmorphism cards.
  - **Branch**: `P4-2`

### Phase 4 Exit Checks
- [ ] The index page loads successfully on the browser.
- [ ] Page layout adjusts dynamically and responsive rules adapt correctly for mobile viewports.

---

## Phase 5: Frontend SPA Routing, State & Charts (`P5`)
- [ ] **P5-1**: Implement routing, navigation events, modal control triggers, and theme toggling persistence inside `src/main/resources/static/js/app.js`.
  - **Branch**: `P5-1`
- [ ] **P5-2**: Integrate Chart.js via CDN link and build helper drawing functions for category donut chart and monthly trend bar chart, handling lifecycle destruction.
  - **Branch**: `P5-2`

### Phase 5 Exit Checks
- [ ] Navigation changes between sections occur smoothly without triggering a full page reload.
- [ ] Theme toggles instantly and preserves state in local storage upon refresh.
- [ ] Empty/mock data charts render correctly on the dashboard page.

---

## Phase 6: API Integration & E2E Validation (`P6`)
- [ ] **P6-1**: Create standard async fetch handler (`apiFetch`) and implement data loading functions for rendering the expense table and search filters in `app.js`.
  - **Branch**: `P6-1`
- [ ] **P6-2**: Connect submit/delete handlers for adding new transactions, editing existing records, removing entries, and updating budget configurations.
  - **Branch**: `P6-2`
- [ ] **P6-3**: Integrate budget threshold styling alerts (progress bar color shifting between green, orange, and red depending on consumption percentages).
  - **Branch**: `P6-3`

### Phase 6 Exit Checks
- [ ] Complete CRUD workflow operates correctly, verifying browser-to-database persistence.
- [ ] UI displays correct color styles and warnings when budget consumption matches warning and critical thresholds.
- [ ] Maven builds successfully compile into a single self-contained executable JAR (`mvn clean package`).
- [ ] Run the jar locally (`java -jar ...`) and complete manual validation checklist.
