# Premium Expense Tracker Web Application

A sleek, self-hosted, single-user personal finance management dashboard designed to track daily expenses, visualize spending trends, and manage monthly budget configurations. Built with a robust Spring Boot backend and a highly responsive, modern glassmorphic vanilla frontend.

---

## ✨ Features

- **📊 Dynamic Dashboard**: Instantly tracks key performance metrics including *Total Spent Overall*, *Current Month Spent*, and *Remaining Budget*.
- **📈 Analytical Charts**: Integrates interactive visualizations using Chart.js:
  - **Category Breakdown Chart**: A Doughnut chart representing spending share per category.
  - **Monthly Trends Chart**: A Bar chart displaying spending history across recent months.
- **🚨 Dynamic Budget Threshold Alerts**: An interactive progress bar changes color and displays alerts dynamically based on consumption:
  - **Safe State (< 80%)**: Cool Blue/Green gradient fill.
  - **Warning State (80% - 99%)**: Amber Orange fill warning that the limit is near.
  - **Over Budget (>= 100%)**: Crimson Red fill accompanied by a global dashboard alert banner.
- **📝 Full CRUD Logs**: Allows users to filter expenses by *Category* and *Month/Year*, add new expenses, edit pre-populated transaction forms in modals, and delete records with a smooth fade-out animation.
- **🌓 Localized Dark/Light Modes**: Instantly toggles between light and dark themes using CSS variables with state persistence in `localStorage` to prevent flashes of unstyled layouts on reload.

---

## 🛠️ Tech Stack

### Backend
- **Core Framework**: Java 21 & Spring Boot 3.3.0
- **Data Access**: Spring Data JPA & Hibernate ORM
- **Object Mapping**: Lombok (boilerplate elimination)
- **Validation**: JSR-380 Bean Validation (`jakarta.validation`)
- **Testing**: JUnit 5, MockMvc, Mockito, AssertJ

### Database & Devops
- **Relational Storage**: PostgreSQL 16 (Alpine image)
- **Containerization**: Docker & Docker Compose
- **CI Pipeline**: GitHub Actions CI workflow (compiling, code checks, automated tests on pull requests)

### Frontend
- **Structure**: Vanilla HTML5 (semantic layout)
- **Styling**: Vanilla CSS3 (curated HSL palettes, glassmorphism, responsive grid, micro-animations)
- **Interactions**: Vanilla JavaScript (ES6+ native SPA router and event listener system)
- **Libraries**: Chart.js (via CDN link)

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK installed
- Docker and Docker Compose installed
- Maven installed (or bundled Maven from IDE)

### 1. Database Initialization
Spin up the containerized PostgreSQL database:
```powershell
docker compose up -d
```
The database executes on port `5432` with username `admin` and password `admin`. On first application launch, JPA Hibernate will automatically generate the schema and seed the default budget settings.

### 2. Compilation and Packaging
Compile the application resources and bundle the frontend SPA directly into the executable package:
```powershell
mvn clean package
```
*Note: If utilizing a bundled Maven binary (e.g. from IntelliJ), execute using the absolute path to the wrapper/binary.*

### 3. Run the Application
Execute the compiled self-contained JAR:
```powershell
java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar
```

Open a web browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🧪 Testing

Run the automated integration and unit test suite (covering entities, repositories, service layers, global exception handlers, and controller endpoints):
```powershell
mvn test
```

---

## 🔄 Development Workflows

This repository follows a strict workflow to ensure code quality and prevent developer bias:
- **Branching Strategy**: All tasks are executed on dedicated branches named after their corresponding task IDs (e.g., `P1-2`).
- **Independent PR Review**: To prevent self-review bias, implementing agents/developers MUST NOT review or merge their own PRs. A separate `pr_reviewer` subagent is spawned to inspect diffs against design specifications, post review comments, and approve changes. The implementing agent must fix any issues and is responsible for squash-merging once approved.
- Refer to [docs/pr-review-process.md](file:///C:/Users/ALBIN/Desktop/main/Albin/DEV/agy/docs/pr-review-process.md) for full review process guidelines.
