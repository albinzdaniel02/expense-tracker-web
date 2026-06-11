# Solution Design Document - Expense Tracker

This document presents the detailed design for the self-hosted **Expense Tracker** application. It aligns the functional and non-functional requirements with the technical architecture.

---

## 1. Solution Overview

The Expense Tracker is a self-hosted, lightweight personal finance application built to run on a single machine with zero external hosting dependencies.

### 1.1. Core Architectural Pillars
*   **Single-User Focus**: The application is tailored for a single occupant. There are no registrations, sign-ins, sessions, or multi-tenant database partitioning. Anyone with network access to the host machine views the same dashboard and data.
*   **No Authentication**: By bypassing user authentication libraries (such as Spring Security) and JWT tokens, the design minimizes startup overhead, avoids database management of credentials, and offers immediate utility upon application launch.
*   **Local Self-Hosting**: The application runs on a local port (e.g., `http://localhost:8080`). 
    *   The frontend files (HTML, CSS, JS) are packaged directly into the jar file (`src/main/resources/static/`) and served statically by Spring Boot.
    *   The backend runs locally on the JVM.
    *   The relational database is containerized locally, separating database persistence from the host OS system configuration.

---

## 2. Backend Logic

The backend is built with Java 21 and Spring Boot 3.x, utilizing Spring Data JPA for object-relational mapping to a PostgreSQL database.

### 2.1. Entity Design
1.  **`Expense`**: Maps directly to the `expense` table.
    *   `id` (`UUID`): Automatically generated universally unique identifier.
    *   `amount` (`BigDecimal`): Stored with a scale of 2 (`numeric(12,2)`) to ensure exact monetary precision without floating-point rounding errors.
    *   `category` (`String`): Standard classification category (e.g., Food, Transport, Utilities, Entertainment, Shopping, Others).
    *   `expenseDate` (`LocalDate`): Stored as SQL `DATE`. Defaults to the current date unless specified by the user.
2.  **`BudgetSettings`**: Maps to the `budget_settings` table.
    *   `id` (`UUID`): Set to a fixed constant UUID (`00000000-0000-0000-0000-000000000000`) as a singleton identifier.
    *   `monthlyLimit` (`BigDecimal`): Monthly budget limit stored as `numeric(12,2)`.

### 2.2. CRUD Operations
*   **Expenses (`ExpenseController`)**:
    *   `POST /api/expenses`: Validates inputs (amount > 0, non-blank category, non-null date), generates a UUID (or lets the DB/JPA generate it), and saves the new record.
    *   `GET /api/expenses`: Retrieves lists of expenses. Allows filtering by:
        *   `category` (exact match).
        *   `monthYear` (formatted as `YYYY-MM`), querying dates falling between the first day and last day of that month.
    *   `PUT /api/expenses/{id}`: Finds the expense by UUID, updates its fields, and saves it.
    *   `DELETE /api/expenses/{id}`: Removes the expense record after validating existence.
*   **Budget Settings (`BudgetController`)**:
    *   `GET /api/budget-settings`: Retrieves the single configuration record (where `id = 00000000-0000-0000-0000-000000000000`). If the database has no records (e.g., first-time launch), the service automatically seeds a default budget row (e.g., `$1000.00`) and returns it.
    *   `PUT /api/budget-settings`: Accepts a new `monthlyLimit` in the JSON body, retrieves the record for `id = 00000000-0000-0000-0000-000000000000`, updates it, and saves it.

### 2.3. Statistics & Aggregation Computations
*   **Current Month Spend**: 
    Calculated via a custom JPA repository query summing the expense amounts between the first and last day of the current calendar month:
    $$\text{Current Month Spend} = \sum_{d = \text{First Day}}^{\text{Last Day}} \text{Expense.amount}$$
    *SQL Representation:*
    ```sql
    SELECT COALESCE(SUM(amount), 0) 
    FROM expense 
    WHERE expense_date >= :firstDayOfMonth AND expense_date <= :lastDayOfMonth;
    ```
*   **Category Breakdown**: 
    Aggregates expenses grouped by category for a specific month.
    *SQL Representation:*
    ```sql
    SELECT category, SUM(amount) AS total 
    FROM expense 
    WHERE expense_date >= :firstDayOfMonth AND expense_date <= :lastDayOfMonth
    GROUP BY category 
    ORDER BY total DESC;
    ```
*   **Monthly Trend**: 
    Extracts the summary totals for the last $N$ calendar months (typically $6$ months) to display chronologically.
    *SQL Representation:*
    ```sql
    SELECT 
        EXTRACT(YEAR FROM expense_date) AS yr, 
        EXTRACT(MONTH FROM expense_date) AS mon, 
        SUM(amount) AS total 
    FROM expense 
    GROUP BY yr, mon 
    ORDER BY yr DESC, mon DESC 
    LIMIT 6;
    ```

---

## 3. Frontend Design

The frontend is a modern Single Page Application (SPA) built using standard web technologies with no build-step required (no Webpack, Vite, or React/Vue needed).

### 3.1. Single Page Application (SPA) Structure
The entire app layout is declared in a single `index.html` file, structured using semantic HTML tags (`<header>`, `<nav>`, `<main>`, `<section>`).

*   **View Containers**: Different application screens are represented by discrete container elements:
    *   `#dashboard-view`: Displays KPI cards (Total Spent, Month Spent, Remaining Budget), budget progress, and analytical charts.
    *   `#expenses-view`: Contains search/filter controls, the tabular display of records, and forms to add/edit entries.
    *   `#settings-view`: Contains a form to change the monthly budget limit.
*   **Routing and State Management**: 
    *   In `app.js`, a centralized router listens to clicks on navigation links (`data-target` attributes) and manages views.
    *   View switching is performed by toggling a CSS helper class `.hidden { display: none !important; }` on the container sections.
    *   The JS script maintains a local state object holding cached expenses, current filters, active budget limit, and current view state.

### 3.2. Theme Toggling
*   **CSS Custom Properties**: Define the color themes at the `:root` level.
    ```css
    :root {
        /* Default Light Theme variables */
        --bg-color: #f8fafc;
        --card-bg: #ffffff;
        --text-primary: #1e293b;
        --text-secondary: #64748b;
        --border-color: #e2e8f0;
        --accent-color: #4f46e5;
    }
    
    [data-theme="dark"] {
        /* Dark Theme overrides */
        --bg-color: #0f172a;
        --card-bg: #1e293b;
        --text-primary: #f8fafc;
        --text-secondary: #94a3b8;
        --border-color: #334155;
        --accent-color: #818cf8;
    }
    ```
*   **Persistence**:
    1.  When the toggle button is clicked, the JS script toggles the `data-theme` attribute on the `<html>` element.
    2.  The active selection is saved using `localStorage.setItem('theme', 'dark')` (or `'light'`).
    3.  Upon initial page load, `localStorage.getItem('theme')` is checked. If found, it is applied immediately to prevent flash-of-unstyled-content (FOUC).

### 3.3. Chart.js Implementation
Chart.js is loaded dynamically via CDN in the HTML header. The frontend initializes two main visualizations:
1.  **Category Distribution**: A donut chart representing the percentage allocation of the current month's spending.
2.  **Spending Trends**: A vertical bar chart illustrating the monthly totals over the last 6 months.

*   **Instance Lifecycle Management**:
    To prevent canvas overlay rendering issues and hover-interaction errors where elements flicker back and forth, any existing Chart instance must be disposed of before rendering updated data.
    ```javascript
    let categoryChartInstance = null;
    let trendChartInstance = null;

    function renderCategoryChart(data) {
        if (categoryChartInstance !== null) {
            categoryChartInstance.destroy();
        }
        
        const ctx = document.getElementById('categoryChart').getContext('2d');
        categoryChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: data,
            options: { /* ... */ }
        });
    }
    ```

---

## 4. Budget Tracking & Alerts

The application computes and communicates budget status in real-time.

### 4.1. Calculations
Budget consumption is calculated locally in the frontend as well as summarized on the backend:
$$\text{Budget Consumption \%} = \left( \frac{\text{Current Month Spend}}{\text{Monthly Budget Limit}} \right) \times 100$$
$$\text{Remaining Budget} = \text{Monthly Budget Limit} - \text{Current Month Spend}$$

### 4.2. Status Thresholds & Visual Styling
To facilitate rapid recognition of financial health, the app styles visual widgets (progress bars, background badges, budget card borders) using three distinct color schemes based on the percentage consumed:

| Level | Percentage Range | Classification | CSS Custom Variables / Hex Codes | UX Alert Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **Safe** | $< 80\%$ | Cool / Healthy | Green/Blue (`#10B981` / `#3B82F6`) | Standard styling, positive progress bar indicator. |
| **Warning** | $80\% - 99.9\%$ | Caution | Orange/Yellow (`#F59E0B` / `#F59E0B`) | Progress bar changes color; warns user that spending limit is near. |
| **Over Budget** | $\ge 100\%$ | Critical | Crimson Red (`#EF4444`) | Progress bar shows full width in bright red. An alert banner appears on the dashboard dashboard. |

### 4.3. Dynamic Progress Bar Logic
Whenever an expense is added, edited, or deleted, or when the monthly budget settings are modified:
1.  The frontend fetches the fresh budget status.
2.  The percentage is re-calculated.
3.  The width of the progress element is updated: `progressBar.style.width = Math.min(percentage, 100) + '%';`.
4.  Class lists are updated dynamically:
    ```javascript
    progressBar.classList.remove('bg-safe', 'bg-warning', 'bg-critical');
    if (percentage < 80) {
        progressBar.classList.add('bg-safe');
    } else if (percentage < 100) {
        progressBar.classList.add('bg-warning');
    } else {
        progressBar.classList.add('bg-critical');
    }
    ```
