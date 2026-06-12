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

// Global Chart Instances
let categoryChartInstance = null;
let trendChartInstance = null;

// Initialize Application when DOM is fully loaded
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

// Theme Toggling Logic
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
}

function setTheme(theme) {
    state.theme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
}

function toggleTheme() {
    setTheme(state.theme === 'light' ? 'dark' : 'light');
    if (state.currentView === 'dashboard-view') {
        refreshDashboard();
    }
}

// Event Listeners Setup
function setupEventListeners() {
    // Navigation link clicks
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(item.getAttribute('data-target'));
        });
    });

    // Theme Toggle
    const themeBtn = document.getElementById('theme-toggle-btn');
    if (themeBtn) {
        themeBtn.addEventListener('click', toggleTheme);
    }

    // Modal Control triggers
    const addBtn = document.getElementById('open-add-modal-btn');
    if (addBtn) addBtn.addEventListener('click', () => openExpenseModal());

    const closeBtn = document.getElementById('close-modal-btn');
    if (closeBtn) closeBtn.addEventListener('click', closeExpenseModal);

    const cancelBtn = document.getElementById('cancel-modal-btn');
    if (cancelBtn) cancelBtn.addEventListener('click', closeExpenseModal);
    
    // Expense form submission
    const expenseForm = document.getElementById('expense-form');
    if (expenseForm) expenseForm.addEventListener('submit', handleExpenseSubmit);

    // Filter Listeners
    const filterMonth = document.getElementById('filter-month');
    if (filterMonth) {
        // Set default filter value to current month
        filterMonth.value = state.filters.monthYear;
        filterMonth.addEventListener('input', (e) => {
            state.filters.monthYear = e.target.value;
            refreshExpensesLog();
        });
    }
    
    const filterCategory = document.getElementById('filter-category');
    if (filterCategory) {
        filterCategory.addEventListener('change', (e) => {
            state.filters.category = e.target.value;
            refreshExpensesLog();
        });
    }
    
    const clearFiltersBtn = document.getElementById('clear-filters-btn');
    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', () => {
            if (filterCategory) filterCategory.value = "";
            const currentMonthString = new Date().toISOString().substring(0, 7);
            if (filterMonth) filterMonth.value = currentMonthString;
            state.filters.category = "";
            state.filters.monthYear = currentMonthString;
            refreshExpensesLog();
        });
    }

    // Settings form submission
    const settingsForm = document.getElementById('settings-form');
    if (settingsForm) settingsForm.addEventListener('submit', handleSettingsSubmit);
}

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
        console.error("Failed to fetch expenses: ", error);
        // Fallback to empty table on error
        renderExpenseTable([]);
    }
}

// Render dynamic rows with UUID bindings
function renderExpenseTable(expenses) {
    const tbody = document.getElementById('expense-table-body');
    const msg = document.getElementById('no-records-msg');
    if (!tbody || !msg) return;
    
    tbody.innerHTML = '';
    
    if (expenses.length === 0) {
        msg.classList.remove('hidden');
        return;
    }
    msg.classList.add('hidden');

    expenses.forEach(exp => {
        const tr = document.createElement('tr');
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

// Modal Forms, Edit, and Delete Lifecycle
function openExpenseModal(expense = null) {
    const modal = document.getElementById('expense-modal');
    const title = document.getElementById('modal-title');
    if (!modal || !title) return;
    
    document.getElementById('expense-id-input').value = expense ? expense.id : '';
    document.getElementById('amount-input').value = expense ? expense.amount : '';
    document.getElementById('category-input').value = expense ? expense.category : '';
    document.getElementById('date-input').value = expense ? expense.expenseDate : new Date().toISOString().substring(0, 10);
    
    title.innerText = expense ? 'Edit Expense' : 'Add Expense';
    modal.classList.add('active');
}

function closeExpenseModal() {
    const modal = document.getElementById('expense-modal');
    if (modal) modal.classList.remove('active');
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

// Dashboard Summary & Charts Updates
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
        if (barFill) {
            barFill.style.width = `${Math.min(percentage, 100)}%`;
            document.getElementById('progress-percent-text').innerText = `${percentage.toFixed(2)}% Spent`;
            document.getElementById('progress-desc-text').innerText = `$${summary.currentMonthSpend.toFixed(2)} spent of $${summary.budgetLimit.toFixed(2)} Limit`;

            // Update Alert threshold coloring
            barFill.classList.remove('bg-safe', 'bg-warning', 'bg-critical');
            const alertBanner = document.getElementById('global-alert-banner');
            
            if (percentage < 80.00) {
                barFill.classList.add('bg-safe');
                if (alertBanner) alertBanner.classList.add('hidden');
            } else if (percentage < 100.00) {
                barFill.classList.add('bg-warning');
                if (alertBanner) alertBanner.classList.add('hidden');
            } else {
                barFill.classList.add('bg-critical');
                if (alertBanner) alertBanner.classList.remove('hidden');
            }
        }

        // Load analytical charts
        await updateCategoryChart();
        await updateTrendChart();
        
    } catch (error) {
        console.error("Dashboard refresh error: ", error);
        // If API fails or backend is not seeded/running yet, render fallback/empty state
        renderFallbackDashboard();
    }
}

function renderFallbackDashboard() {
    document.getElementById('total-spent-val').innerText = '$0.00';
    document.getElementById('month-spent-val').innerText = '$0.00';
    document.getElementById('remaining-budget-val').innerText = '$0.00';
    
    const currentMonthString = new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
    document.getElementById('current-month-lbl').innerText = `${currentMonthString} Total`;
    document.getElementById('budget-month-lbl').innerText = `${currentMonthString} Target`;

    const barFill = document.getElementById('progress-bar-fill');
    if (barFill) {
        barFill.style.width = '0%';
        document.getElementById('progress-percent-text').innerText = '0.00% Spent';
        document.getElementById('progress-desc-text').innerText = '$0.00 spent of $0.00 Limit';
        barFill.classList.remove('bg-warning', 'bg-critical');
        barFill.classList.add('bg-safe');
    }
    const alertBanner = document.getElementById('global-alert-banner');
    if (alertBanner) alertBanner.classList.add('hidden');

    renderEmptyCharts();
}

// Chart.js Category Breakdown rendering
async function updateCategoryChart() {
    try {
        const data = await apiFetch('/api/expenses/stats/category-breakdown');
        renderCategoryBreakdownChart(data);
    } catch (error) {
        console.error("Failed to load category chart: ", error);
        renderCategoryBreakdownChart([]);
    }
}

function renderCategoryBreakdownChart(data) {
    const labels = data.length > 0 ? data.map(item => item.category) : ['No Data'];
    const amounts = data.length > 0 ? data.map(item => item.totalAmount) : [0.00];

    if (categoryChartInstance) {
        categoryChartInstance.destroy();
    }

    const chartCanvas = document.getElementById('categoryChart');
    if (!chartCanvas) return;
    
    const ctx = chartCanvas.getContext('2d');
    categoryChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: amounts,
                backgroundColor: data.length > 0 
                    ? ['#4f46e5', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1', '#64748b']
                    : ['#e2e8f0'],
                borderWidth: state.theme === 'dark' ? 2 : 1,
                borderColor: state.theme === 'dark' ? '#1e293b' : '#ffffff',
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '75%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: state.theme === 'dark' ? '#cbd5e1' : '#475569',
                        font: {
                            family: 'Outfit',
                            size: 12
                        }
                    }
                }
            }
        }
    });
}

// Chart.js Trends rendering
async function updateTrendChart() {
    try {
        const data = await apiFetch('/api/expenses/stats/monthly-trends');
        renderMonthlyTrendsChart(data);
    } catch (error) {
        console.error("Failed to load trend chart: ", error);
        renderMonthlyTrendsChart([]);
    }
}

function renderMonthlyTrendsChart(data) {
    const labels = data.length > 0 ? data.map(item => item.monthYear) : [new Date().toISOString().substring(0, 7)];
    const amounts = data.length > 0 ? data.map(item => item.totalAmount) : [0.00];

    if (trendChartInstance) {
        trendChartInstance.destroy();
    }

    const chartCanvas = document.getElementById('trendChart');
    if (!chartCanvas) return;

    const ctx = chartCanvas.getContext('2d');
    trendChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Monthly Spending',
                data: amounts,
                backgroundColor: data.length > 0 ? 'rgba(79, 70, 229, 0.6)' : 'rgba(226, 232, 240, 0.6)',
                borderColor: data.length > 0 ? '#4f46e5' : '#cbd5e1',
                borderWidth: 1,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: state.theme === 'dark' ? '#cbd5e1' : '#475569',
                        font: { family: 'Outfit' }
                    },
                    grid: {
                        color: state.theme === 'dark' ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    ticks: {
                        color: state.theme === 'dark' ? '#cbd5e1' : '#475569',
                        font: { family: 'Outfit' }
                    },
                    grid: { display: false }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}

function renderEmptyCharts() {
    renderCategoryBreakdownChart([]);
    renderMonthlyTrendsChart([]);
}

// Budget configuration forms
async function loadSettingsForm() {
    try {
        const settings = await apiFetch('/api/budget-settings');
        state.budgetLimit = settings.monthlyLimit;
        const budgetInput = document.getElementById('budget-limit-input');
        if (budgetInput) budgetInput.value = settings.monthlyLimit;
    } catch (error) {
        console.error("Failed to load budget configuration: ", error);
        // Fallback placeholder
        const budgetInput = document.getElementById('budget-limit-input');
        if (budgetInput) budgetInput.value = state.budgetLimit;
    }
}

async function handleSettingsSubmit(e) {
    e.preventDefault();
    const limitInput = document.getElementById('budget-limit-input');
    if (!limitInput) return;
    const newLimit = parseFloat(limitInput.value);
    
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
