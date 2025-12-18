// API Configuration - loaded from config.js
const API_BASE_URL = window.API_BASE_URL || 'http://localhost:8085/api';
let currentUser = null;

// Utility Functions
function getAuthToken() {
    return localStorage.getItem('jwtToken');
}

function isAuthenticated() {
    return !!getAuthToken();
}

function redirectToLogin() {
    if (!isAuthenticated()) {
        window.location.href = 'login.html';
    }
}

function getAuthHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getAuthToken()}`
    };
}

// API Functions
const api = {
    // Auth
    async register(username, password, email) {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email })
        });
        if (!response.ok) throw new Error('Registration failed');
        return response.text();
    },

    async login(username, password) {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email: '' })
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Invalid credentials');
        }
        const data = await response.json();
        return data.token;
    },

    // User
    async getCurrentUser() {
        const response = await fetch(`${API_BASE_URL}/users/me`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch user');
        return response.json();
    },

    // Transactions
    async getTransactions() {
        const response = await fetch(`${API_BASE_URL}/transactions`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch transactions');
        return response.json();
    },

    async addTransaction(transaction) {
        const response = await fetch(`${API_BASE_URL}/transactions`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(transaction)
        });
        if (!response.ok) throw new Error('Failed to add transaction');
        return response.json();
    },

    async filterTransactions(category, from, to) {
        const params = new URLSearchParams();
        if (category) params.append('category', category);
        if (from) params.append('from', from);
        if (to) params.append('to', to);
        const response = await fetch(`${API_BASE_URL}/transactions/filter?${params}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to filter transactions');
        return response.json();
    },

    // Expenses
    async getExpenses() {
        const response = await fetch(`${API_BASE_URL}/expenses/my`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch expenses');
        return response.json();
    },

    async addExpense(expense) {
        const response = await fetch(`${API_BASE_URL}/expenses`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(expense)
        });
        if (!response.ok) throw new Error('Failed to add expense');
        return response.json();
    },

    // Incomes
    async getIncomes(userId) {
        const response = await fetch(`${API_BASE_URL}/incomes/user/${userId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch incomes');
        return response.json();
    },

    async addIncome(income) {
        const response = await fetch(`${API_BASE_URL}/incomes`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(income)
        });
        if (!response.ok) throw new Error('Failed to add income');
        return response.json();
    },

    // Categories
    async getCategories() {
        const response = await fetch(`${API_BASE_URL}/category`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch categories');
        return response.json();
    },

    async addCategory(category) {
        const response = await fetch(`${API_BASE_URL}/category`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(category)
        });
        if (!response.ok) throw new Error('Failed to add category');
        return response.json();
    },

    // Summary
    async getMonthlySummary(month, year) {
        const response = await fetch(`${API_BASE_URL}/summary/monthly?month=${month}&year=${year}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch summary');
        return response.json();
    },

    async getYearlySummary(year) {
        const response = await fetch(`${API_BASE_URL}/summary/yearly?year=${year}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch yearly summary');
        return response.json();
    },

    async getBudgetAlerts(userId, month, year) {
        const response = await fetch(`${API_BASE_URL}/summary/budget-alerts?userId=${userId}&month=${month}&year=${year}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch budget alerts');
        return response.json();
    }
};

// UI Helper Functions
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR'
    }).format(amount);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        setTimeout(() => {
            errorDiv.style.display = 'none';
        }, 5000);
    } else {
        alert(message);
    }
}

function showSuccess(message) {
    const successDiv = document.getElementById('successMessage');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
        setTimeout(() => {
            successDiv.style.display = 'none';
        }, 3000);
    }
}

function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<div class="loading">Loading...</div>';
    }
}

function logout() {
    localStorage.removeItem('jwtToken');
    window.location.href = 'login.html';
}

// Initialize user on page load
async function initUser() {
    if (isAuthenticated()) {
        try {
            currentUser = await api.getCurrentUser();
            updateUserDisplay();
        } catch (error) {
            console.error('Failed to load user:', error);
            logout();
        }
    }
}

function updateUserDisplay() {
    const userDisplay = document.getElementById('userDisplay');
    if (userDisplay && currentUser) {
        userDisplay.textContent = currentUser.username || 'User';
    }
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    initUser();
});
