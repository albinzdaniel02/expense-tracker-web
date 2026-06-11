# UI Wireframes & Layout Specification - Expense Tracker

This document outlines the visual layouts, UI component hierarchy, and design specifications for the self-hosted **Expense Tracker** Single-Page Application (SPA). It aligns directly with the functional requirements, API contract specifications, and technical architecture.

---

## 1. Introduction & Architecture Alignment

The user interface is a responsive, modern Single-Page Application designed for a self-hosted single-user environment. It operates without authentication and dynamically switches views by toggling visual visibility.

### 1.1. Client-Side SPA Mechanics
- **Dynamic View Toggling**: Controlled via JavaScript by adding/removing the `.hidden` class (which applies `display: none !important;`) to sections: `#dashboard-view`, `#expenses-view`, and `#settings-view`.
- **Theme Management**: Switching between Light and Dark mode operates by toggling the `data-theme` attribute on the root `<html>` node. Custom CSS properties (variables) dynamically update colors with smooth transition effects. Preference is stored in the browser's `localStorage`.
- **UUID Data Binding**: Since the backend schema utilizes UUIDs as primary keys, all elements representing data records in the DOM (like table rows) are decorated with data-attributes (e.g. `data-id="<UUID>"`). This integrates frontend interaction directly with REST endpoints (such as `PUT /api/expenses/{id}` and `DELETE /api/expenses/{id}`).

---

## 2. Overall Layout & Application Shell

The application layout is structured inside a global container wrapping a persistent, sticky Header and the main dynamic content viewport.

### 2.1. Overall Layout Shell (ASCII Diagram)
```text
+-----------------------------------------------------------------------------------------+
| [Logo] ExpenseTracker                         [Dashboard]   [Expenses]   [Settings]  (O) |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|  <MAIN DYNAMIC VIEW CONTAINER: #dashboard-view, #expenses-view, or #settings-view>      |
|                                                                                         |
|  +-----------------------------------------------------------------------------------+  |
|  | (Modal Backdrop Overlay - Hidden by default)                                      |  |
|  | +-------------------------------------------------------------------------------+ |  |
|  | | Add/Edit Expense Modal Form (#expense-modal)                                  | |  |
|  | +-------------------------------------------------------------------------------+ |  |
|  +-----------------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------------+
```

### 2.2. Shell Components
- **Logo / App Title**: Clicking this navigates the user back to the Dashboard view.
- **Nav Links**: Tab items (`Dashboard`, `Expenses`, `Settings`) that trigger click event handlers in JavaScript, updating the active view container without resetting page state.
- **Theme Toggle (O)**: A styled icon button (Sun/Moon symbol) mapping to the theme switcher.

---

## 3. View 1: Dashboard (`#dashboard-view`)

The Dashboard is the landing interface, giving an instant overview of spending metrics, budget consumption, and visual breakdown trends.

### 3.1. Dashboard Layout (ASCII Diagram)
```text
+-----------------------------------------------------------------------------------------+
|  DASHBOARD                                                                              |
|                                                                                         |
|  +--------------------+   +--------------------+   +--------------------+               |
|  | TOTAL SPENT        |   | CURRENT MONTH      |   | REMAINING BUDGET   |               |
|  | $3,450.75          |   | $420.50            |   | $1,079.50          |               |
|  | Overall            |   | June 2026          |   | June 2026          |               |
|  +--------------------+   +--------------------+   +--------------------+               |
|                                                                                         |
|  BUDGET PROGRESS (June 2026)                                                            |
|  +-----------------------------------------------------------------------------------+  |
|  |██████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░|  |
|  +-----------------------------------------------------------------------------------+  |
|   28.03% Spent ($420.50 of $1,500.00 Limit)                          [Status: Healthy]  |
|                                                                                         |
|  CHARTS GRID                                                                            |
|  +-------------------------------------+   +-------------------------------------+      |
|  | Category Breakdown (Doughnut)       |   | Monthly Trend (6-Month Bar Chart)   |      |
|  |                                     |   |                                     |      |
|  |               .-''-.                |   |   $1250 |               █           |      |
|  |             .'  Food'.              |   |   $1000 |         █     █           |      |
|  |            / Utilities\             |   |    $750 |   █  █  █  █  █           |      |
|  |           |     ()     |            |   |    $500 |   █  █  █  █  █  █        |      |
|  |            \  Entert. /             |   |    $250 |   █  █  █  █  █  █        |      |
|  |             '.      .'              |   |      $0 +---+---+---+---+---+---+--      |
|  |               '-..-'                |   |        Jan Feb Mar Apr May Jun          |      |
|  +-------------------------------------+   +-------------------------------------+      |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 3.2. Detailed Component Breakdown
- **KPI Metrics Cards Grid**: A responsive grid containing three cards:
  1. **Total Spent Overall**: Sum of all expenses in the system.
  2. **Current Month Spent**: Sum of all expenses matching the current calendar month.
  3. **Remaining Budget**: Target Monthly Budget Limit minus Current Month Spent.
- **Budget Progress Bar**:
  - Displays current month spending progress relative to the budget limit.
  - **Dynamic Theme/Alert Thresholds**:
    - **Safe (Healthy)**: Spent percentage $< 80\%$. Bar is filled with a Blue/Green gradient (`#10B981` / `#3B82F6`).
    - **Warning**: Spent percentage $80\% - 99.9\%$. Bar changes to an Orange/Yellow gradient (`#F59E0B`). Warns user that the budget limit is approaching.
    - **Over Budget**: Spent percentage $\ge 100\%$. Bar turns red (`#EF4444`) and a warning notification banner renders above the KPI metrics.
- **Charts Grid**:
  - Two glassmorphic card containers wrapping standard HTML `<canvas>` targets.
  - Handled by `Chart.js` via CDN. JS logic handles chart rendering and manages instance lifecycles (destroying previous instances on data changes to prevent rendering conflicts).
  - **Doughnut Chart**: Renders category breakdown for the selected month/period.
  - **Bar Chart**: Renders spending trends over the last 6 months.

---

## 4. View 2: Expenses (`#expenses-view`)

The Expenses view contains controls to filter recorded items, launch the expense creation modal, and manage entries through a structured data list.

### 4.1. Expenses Layout (ASCII Diagram)
```text
+-----------------------------------------------------------------------------------------+
|  EXPENSES                                                                               |
|                                                                                         |
|  FILTER & ACTION BAR                                                                    |
|  +-----------------------------------------------------------------------------------+  |
|  | Month/Year: [ June 2026  v ]  Category: [ All Categories v ]   [+ Add Expense]    |  |
|  +-----------------------------------------------------------------------------------+  |
|                                                                                         |
|  EXPENSES LIST                                                                          |
|  +-----------------------------------------------------------------------------------+  |
|  | Date       | Category       | Amount    | Actions                                 |  |
|  +------------+----------------+-----------+-----------------------------------------+  |
|  | 2026-06-10 | Food           | $25.50    | [Edit (Icon)] [Delete (Icon)]           |  |
|  |            |                |           | (data-id: e3b0c442-98fc-1c14-...)       |  |
|  +------------+----------------+-----------+-----------------------------------------+  |
|  | 2026-06-08 | Utilities      | $120.00   | [Edit (Icon)] [Delete (Icon)]           |  |
|  |            |                |           | (data-id: 3f7c81d3-4629-4b68-...)       |  |
|  +------------+----------------+-----------+-----------------------------------------+  |
|  | 2026-06-05 | Entertainment  | $45.00    | [Edit (Icon)] [Delete (Icon)]           |  |
|  |            |                |           | (data-id: 7d5e4b2d-9c3f-42a1-...)       |  |
|  +------------+----------------+-----------+-----------------------------------------+  |
|  | Page 1 of 5                             |                   < Previous  [1] Next >|  |
|  +-----------------------------------------------------------------------------------+  |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 4.2. Component Interaction & UUID Integration
- **Filter Bar**: Contains select dropdowns for filtering items by Category (e.g. Food, Utilities, Shopping) and Month/Year (formatted as `YYYY-MM`). Changing filters triggers a new fetch request: `GET /api/expenses?category={cat}&monthYear={yyyy-mm}`.
- **Add Expense Button**: Triggers the modal form to open in "Create" mode (with a blank transaction ID).
- **Expense Table Layout**:
  - Renders a clean grid list displaying: Date (`YYYY-MM-DD`), Category, Amount, and CRUD Action buttons.
  - **UUID Mapping**: Each `<tr>` or list card wrapper incorporates the entity's UUID in a `data-id` attribute. For example:
    ```html
    <tr data-id="e3b0c442-98fc-1c14-9afb-f3b0c44298fc">
      <td>2026-06-10</td>
      <td>Food</td>
      <td>$25.50</td>
      <td>
        <button class="edit-btn"><i class="icon-edit"></i></button>
        <button class="delete-btn"><i class="icon-trash"></i></button>
      </td>
    </tr>
    ```
  - **Edit Logic**: Clicking edit retrieves the UUID from the row, loads the record details from local memory cache or state, populates the modal form fields, and updates the hidden modal input field `expense-id`.
  - **Delete Logic**: Clicking delete triggers a custom modal confirmation. Upon confirmation, a `DELETE /api/expenses/e3b0c442-98fc-1c14-9afb-f3b0c44298fc` request is made. If successful (`204 No Content`), the row is removed from the DOM with a fade transition.

---

## 5. Add / Edit Expense Form (Modal Backdrop Overlay)

The Add/Edit form displays inside a modal window overlaying the primary workspace.

### 5.1. Modal Form Layout (ASCII Diagram)
```text
+-----------------------------------------------------------------+
|                        [ Add/Edit Expense ]                 [X] |
+-----------------------------------------------------------------+
|                                                                 |
|   Amount ($)*                                                   |
|   +---------------------------------------------------------+   |
|   | 45.00                                                   |   |
|   +---------------------------------------------------------+   |
|                                                                 |
|   Category*                                                     |
|   +---------------------------------------------------------+   |
|   | Entertainment                                         v |   |
|   +---------------------------------------------------------+   |
|                                                                 |
|   Date*                                                         |
|   +---------------------------------------------------------+   |
|   | 2026-06-10                                            # |   |
|   +---------------------------------------------------------+   |
|                                                                 |
|   * Required Fields                                             |
|   (Hidden Input: expenseId = "7d5e4b2d-9c3f-42a1-8d2a-...")      |
|                                                                 |
|                     [ Cancel ]            [ Save Expense ]      |
+-----------------------------------------------------------------+
```

### 5.2. Fields and Validations
- **Hidden Input Field**: Holds the UUID value when editing an existing transaction.
- **Amount Input**: Decimal number entry. Required, must be greater than zero, limited to two decimal places.
- **Category Select**: Dropdown listing preset categories: *Food, Transport, Utilities, Entertainment, Shopping, Others*.
- **Date Picker**: HTML5 date input component. Required, defaults to the current date on initialization.
- **Form Submission Logic**:
  - Checks validations.
  - If `expenseId` is empty: Submits a payload via `POST /api/expenses` to create a record.
  - If `expenseId` contains a UUID: Submits a payload via `PUT /api/expenses/{uuid}` to update the entry.
  - Closes modal, refreshes expense data, and recalculates dashboard summaries.

---

## 6. View 3: Settings (`#settings-view`)

The Settings view provides simple entry fields to modify configuration options.

### 6.1. Settings Layout (ASCII Diagram)
```text
+-----------------------------------------------------------------------------------------+
|  SETTINGS                                                                               |
|                                                                                         |
|  +-----------------------------------------------------------------------------------+  |
|  | MONTHLY BUDGET LIMIT                                                              |  |
|  | Configure the spending target limit for budget tracking.                          |  |
|  | (Settings Record UUID: 00000000-0000-0000-0000-000000000000)                      |  |
|  |                                                                                   |  |
|  | Budget Limit ($)*                                                                 |  |
|  | +-------------------------------------------------------------------------------+ |  |
|  | | 1500.00                                                                       | |  |
|  | +-------------------------------------------------------------------------------+ |  |
|  |                                                                                   |  |
|  | [ Save Settings ]                                                                 |  |
|  +-----------------------------------------------------------------------------------+  |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 6.2. Singleton Budget Configuration & API Interactions
- **Singleton UUID Design**: The system maintains a single global settings row. To fetch and update this configuration, it uses a fixed constant UUID: `00000000-0000-0000-0000-000000000000`.
- **Retrieval Workflow**: Opening Settings dispatches a `GET /api/budget-settings`. If no settings database row exists (first application run), the backend automatically seeds a default budget of `1000.00` with the constant UUID and returns it.
- **Save Workflow**: Submitting the form posts the new limit via `PUT /api/budget-settings`:
  ```json
  {
    "monthlyLimit": 1500.00
  }
  ```
  Upon saving, the dashboard progress bar, remaining budget calculations, and colors instantly update.

---

## 7. Design Tokens & Styling Specifications

To enforce premium visual aesthetics, the application uses a custom design token system with glassmorphism effects and modern styling properties.

### 7.1. Color System (CSS Variables)
```css
:root {
    /* Font Families */
    --font-sans: 'Outfit', 'Inter', system-ui, -apple-system, sans-serif;
    
    /* Spacing System (8px Grid Scale) */
    --space-2xs: 4px;
    --space-xs: 8px;
    --space-sm: 12px;
    --space-md: 16px;
    --space-lg: 24px;
    --space-xl: 32px;
    --space-2xl: 48px;

    /* Theme: Light Mode Base */
    --bg-main: radial-gradient(circle at top right, #f8fafc, #e2e8f0);
    --glass-bg: rgba(255, 255, 255, 0.45);
    --glass-border: rgba(255, 255, 255, 0.3);
    --glass-shadow: 0 8px 32px 0 rgba(148, 163, 184, 0.15);
    
    --text-main: #0f172a;
    --text-muted: #475569;
    --primary-accent: #4f46e5;       /* Indigo-600 */
    --primary-hover: #4338ca;        /* Indigo-700 */

    /* Alert / Status Semantics */
    --alert-safe: #10b981;           /* Emerald Green */
    --alert-warning: #f59e0b;        /* Amber Orange */
    --alert-critical: #ef4444;       /* Rose Red */
}

[data-theme="dark"] {
    /* Theme: Dark Mode Overrides */
    --bg-main: radial-gradient(circle at top right, #0f172a, #020617);
    --glass-bg: rgba(15, 23, 42, 0.55);
    --glass-border: rgba(255, 255, 255, 0.08);
    --glass-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.4);
    
    --text-main: #f8fafc;
    --text-muted: #94a3b8;
    --primary-accent: #818cf8;       /* Indigo-400 */
    --primary-hover: #6366f1;        /* Indigo-500 */
}
```

### 7.2. Glassmorphic Surface Styling
All primary containers, cards, tables, header navigation, and input groups are styled using glassmorphism properties to create visual depth and a premium look:
```css
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
    box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.2);
}
```

### 7.3. Micro-Animations & Input Styling
- **Button Hover Scale**: Elevates 1-2px and triggers high-contrast glow gradients:
  ```css
  .btn-primary {
      background: linear-gradient(135deg, var(--primary-accent), var(--primary-hover));
      border: none;
      color: #ffffff;
      padding: var(--space-xs) var(--space-md);
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
  ```
- **Form Inputs**: Soft borders that expand and illuminate with an accent shadow on focus (`box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.25)`).
- **Responsive Layout Adjustments**: Fluid Flexbox layouts and CSS Grid systems adapt content containers for desktop, tablet, and mobile views.
