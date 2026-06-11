# API Contracts - Expense Tracker

This document details the REST API endpoints, request/response formats, query parameters, data types, and error handling for the self-hosted Expense Tracker application.

---

## 1. Global API Configuration

### 1.1. Base URL
Since the frontend and backend are packaged together and served from the same JAR file, the frontend can call the REST API using relative paths (e.g., `/api/expenses`). 
For local development, the default base URL of the Spring Boot application is:
```text
http://localhost:8080
```

### 1.2. Common Headers
All request and response bodies must use the JSON format. The following headers should be included in HTTP communications:

*   **Request Headers**:
    *   `Content-Type: application/json`
    *   `Accept: application/json`
*   **Response Headers**:
    *   `Content-Type: application/json`

---

## 2. Expense Endpoints

### 2.1. Retrieve Expenses
Retrieve a list of expense records, with optional filters for category and month-year.

*   **HTTP Method**: `GET`
*   **Path**: `/api/expenses`
*   **Query Parameters**:
    *   `category` (optional, String): Filter by exact category (e.g., `Food`, `Transport`, `Utilities`).
    *   `monthYear` (optional, String): Filter by calendar month. Format: `YYYY-MM` (e.g., `2026-06`).
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON Array)**:
        ```json
        [
          {
            "id": "e3b0c442-98fc-1c14-9afb-f3b0c44298fc",
            "amount": 25.50,
            "category": "Food",
            "expenseDate": "2026-06-10"
          },
          {
            "id": "3f7c81d3-4629-4b68-87a3-e4d693f9c6d4",
            "amount": 120.00,
            "category": "Utilities",
            "expenseDate": "2026-06-08"
          }
        ]
        ```

### 2.2. Create Expense
Create a new expense record in the database.

*   **HTTP Method**: `POST`
*   **Path**: `/api/expenses`
*   **Request Body (JSON)**:
    *   `amount` (BigDecimal, required): Must be a positive decimal.
    *   `category` (String, required): Category text (non-blank).
    *   `expenseDate` (LocalDate, required): Date in `YYYY-MM-DD` format.
    *   **Example**:
        ```json
        {
          "amount": 45.00,
          "category": "Entertainment",
          "expenseDate": "2026-06-10"
        }
        ```
*   **Success Response**:
    *   **Status Code**: `201 Created`
    *   **Payload (JSON)**:
        ```json
        {
          "id": "7d5e4b2d-9c3f-42a1-8d2a-4b2a8d5c4e9f",
          "amount": 45.00,
          "category": "Entertainment",
          "expenseDate": "2026-06-10"
        }
        ```
*   **Error Responses**:
    *   `400 Bad Request`: If input validation fails (e.g., amount is negative, category is empty, date is missing or invalid).

### 2.3. Update Expense
Modify an existing expense record by ID.

*   **HTTP Method**: `PUT`
*   **Path**: `/api/expenses/{id}`
*   **Path Parameters**:
    *   `id` (UUID, required): The database identifier (UUID) of the expense to update.
*   **Request Body (JSON)**: Same format as `POST /api/expenses`.
    *   **Example**:
        ```json
        {
          "amount": 50.00,
          "category": "Entertainment",
          "expenseDate": "2026-06-10"
        }
        ```
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON)**:
        ```json
        {
          "id": "7d5e4b2d-9c3f-42a1-8d2a-4b2a8d5c4e9f",
          "amount": 50.00,
          "category": "Entertainment",
          "expenseDate": "2026-06-10"
        }
        ```
*   **Error Responses**:
    *   `400 Bad Request`: If request body validation fails.
    *   `404 Not Found`: If no expense exists with the specified `id`.

### 2.4. Delete Expense
Remove an expense record by ID.

*   **HTTP Method**: `DELETE`
*   **Path**: `/api/expenses/{id}`
*   **Path Parameters**:
    *   `id` (UUID, required): The database identifier (UUID) of the expense to delete.
*   **Success Response**:
    *   **Status Code**: `204 No Content`
    *   **Payload**: *Empty*
*   **Error Responses**:
    *   `404 Not Found`: If no expense exists with the specified `id`.

---

## 3. Budget Settings Endpoints

Since this is a single-user system, only one monthly budget configuration record exists. This record is stored with a fixed identifier (`id: "00000000-0000-0000-0000-000000000000"`).

### 3.1. Retrieve Budget Settings
Retrieve the current monthly budget limit.

*   **HTTP Method**: `GET`
*   **Path**: `/api/budget-settings`
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON)**:
        ```json
        {
          "id": "00000000-0000-0000-0000-000000000000",
          "monthlyLimit": 1000.00
        }
        ```
    *   **Database Seeding Behavior**: If the database does not contain a budget settings record (e.g., upon first launch), the server must automatically seed a default budget of `1000.00` and return it in this call.

### 3.2. Update Budget Settings
Update the monthly budget limit.

*   **HTTP Method**: `PUT`
*   **Path**: `/api/budget-settings`
*   **Request Body (JSON)**:
    *   `monthlyLimit` (BigDecimal, required): Must be a positive decimal.
    *   **Example**:
        ```json
        {
          "monthlyLimit": 1500.00
        }
        ```
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON)**:
        ```json
        {
          "id": "00000000-0000-0000-0000-000000000000",
          "monthlyLimit": 1500.00
        }
        ```
*   **Error Responses**:
    *   `400 Bad Request`: If `monthlyLimit` is null or invalid (e.g., negative value).

---

## 4. Statistics & Dashboard Endpoints

### 4.1. Retrieve Summary Metrics
Retrieve overall status metrics, current month spending, and remaining budget for the dashboard.

*   **HTTP Method**: `GET`
*   **Path**: `/api/expenses/stats/summary`
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON)**:
        ```json
        {
          "totalSpend": 3450.75,
          "currentMonthSpend": 420.50,
          "remainingBudget": 1079.50,
          "budgetLimit": 1500.00,
          "budgetPercentage": 28.03
        }
        ```
    *   **Calculations**:
        *   `totalSpend`: Sum of all expenses in the system.
        *   `currentMonthSpend`: Sum of expenses where the `expenseDate` falls within the current calendar month.
        *   `remainingBudget`: `budgetLimit` minus `currentMonthSpend` (can be negative if over budget).
        *   `budgetLimit`: The active monthly limit configured in `BudgetSettings`.
        *   `budgetPercentage`: `(currentMonthSpend / budgetLimit) * 100` (rounded to two decimal places).

### 4.2. Retrieve Category Breakdown
Retrieve aggregated spending grouped by category for a specific calendar month.

*   **HTTP Method**: `GET`
*   **Path**: `/api/expenses/stats/category-breakdown`
*   **Query Parameters**:
    *   `monthYear` (optional, String): Target month in `YYYY-MM` format. If omitted, defaults to the current calendar month.
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON Array)**: Sorted in descending order of total spend.
        ```json
        [
          {
            "category": "Food",
            "totalAmount": 250.00
          },
          {
            "category": "Utilities",
            "totalAmount": 120.50
          },
          {
            "category": "Entertainment",
            "totalAmount": 50.00
          }
        ]
        ```

### 4.3. Retrieve Monthly Trends
Retrieve monthly spending totals for the last $N$ months to plot trend data.

*   **HTTP Method**: `GET`
*   **Path**: `/api/expenses/stats/monthly-trends`
*   **Query Parameters**:
    *   `limit` (optional, Integer): Number of months to fetch. Defaults to 6.
*   **Success Response**:
    *   **Status Code**: `200 OK`
    *   **Payload (JSON Array)**: Sorted chronologically (oldest to newest).
        ```json
        [
          {
            "monthYear": "2026-01",
            "totalAmount": 850.00
          },
          {
            "monthYear": "2026-02",
            "totalAmount": 920.00
          },
          {
            "monthYear": "2026-03",
            "totalAmount": 1100.00
          },
          {
            "monthYear": "2026-04",
            "totalAmount": 780.00
          },
          {
            "monthYear": "2026-05",
            "totalAmount": 1250.00
          },
          {
            "monthYear": "2026-06",
            "totalAmount": 420.50
          }
        ]
        ```

---

## 5. Field Types & Formats Reference

| Field | JSON Type | Java Class | Format / Validation |
| :--- | :--- | :--- | :--- |
| `id` | String | `UUID` | Universally unique identifier (standard 36-character UUID string) |
| `amount` | Number | `BigDecimal` | Non-negative decimal, serialized with 2 decimal places |
| `category` | String | `String` | Non-empty, non-blank string |
| `expenseDate` | String | `LocalDate` | String formatted as `YYYY-MM-DD` |
| `monthlyLimit` | Number | `BigDecimal` | Positive decimal, serialized with 2 decimal places |
| `totalSpend` | Number | `BigDecimal` | Overall sum of all expenses |
| `currentMonthSpend` | Number | `BigDecimal` | Sum of expenses in the current month |
| `remainingBudget` | Number | `BigDecimal` | Remaining budget limit |
| `budgetLimit` | Number | `BigDecimal` | Limit set in configurations |
| `budgetPercentage` | Number | `BigDecimal` or `Double` | Percentage representing budget consumption |
| `monthYear` | String | `String` | Format: `YYYY-MM` |
| `totalAmount` | Number | `BigDecimal` | Aggregated total amount per category or month |

---

## 6. HTTP Status Codes & Error Handling

The API uses standard HTTP response status codes to communicate request results. In the event of an error, the response body contains a JSON object mapping the details of the failure.

### 6.1. Main HTTP Status Codes

*   **`200 OK`**: The request was successful, and the response body contains the requested resource.
*   **`201 Created`**: The request was successful, and a new resource was created.
*   **`204 No Content`**: The request was successful, and there is no representation to return (e.g., successful deletion).
*   **`400 Bad Request`**: The request could not be processed due to validation errors (e.g. invalid date formats, negative amount, missing mandatory fields).
*   **`404 Not Found`**: The requested resource does not exist (e.g. looking up a non-existent expense ID).
*   **`500 Internal Server Error`**: An unexpected server-side error occurred.

### 6.2. Error Response Payload
All errors (4xx and 5xx status codes) should return a structured error response matching the following schema:

```json
{
  "timestamp": "2026-06-10T16:24:47Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Expense amount must be greater than zero.",
  "path": "/api/expenses"
}
```

#### Fields:
*   **`timestamp`** (String): ISO-8601 UTC timestamp representing when the error occurred.
*   **`status`** (Number): HTTP status code value (e.g., 400, 404, 500).
*   **`error`** (String): HTTP standard phrase corresponding to the status code.
*   **`message`** (String): A descriptive explanation of why the request failed.
*   **`path`** (String): The relative endpoint URL that triggered the error.
