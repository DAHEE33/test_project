// API 기본 설정
const API_BASE_URL = 'http://localhost:8080';

// API 호출 함수
async function apiCall(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 토큰이 있으면 헤더에 추가
    const token = localStorage.getItem('accessToken');
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }

    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers,
        },
    };

    try {
        const response = await fetch(url, finalOptions);
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            
            // 백엔드에서 전달된 에러 코드 처리
            if (errorData.errorCode) {
                throw new Error(errorData.errorCode);
            } else if (errorData.message) {
                throw new Error(errorData.message);
            } else {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
        }
        
        return await response.json();
    } catch (error) {
        console.error('API 호출 오류:', error);
        throw error;
    }
}

// 인증 관련 API
async function register(phoneNumber, password) {
    return apiCall('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ phoneNumber, password }),
    });
}

async function login(phoneNumber, password) {
    return apiCall('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ phoneNumber, password }),
    });
}

async function logout() {
    return apiCall('/auth/logout', {
        method: 'POST',
    });
}

async function refreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
        throw new Error('Refresh token이 없습니다.');
    }
    
    return apiCall('/auth/refresh', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
    });
}

// 계좌 관련 API
async function getAccountBalance() {
    return apiCall('/account/balance');
}

async function getTransactionHistory(type = 'ALL') {
    return apiCall(`/account/transactions?type=${type}`);
}

async function transfer(recipientAccount, amount, memo) {
    return apiCall('/account/transfer', {
        method: 'POST',
        body: JSON.stringify({ recipientAccount, amount, memo }),
    });
}

async function deposit(amount, memo) {
    return apiCall('/account/deposit', {
        method: 'POST',
        body: JSON.stringify({ amount, memo }),
    });
}

async function withdraw(amount, memo) {
    return apiCall('/account/withdraw', {
        method: 'POST',
        body: JSON.stringify({ amount, memo }),
    });
}

// 결제 관련 API
async function makePayment(merchantName, amount, category) {
    return apiCall('/account/payment', {
        method: 'POST',
        body: JSON.stringify({ merchantName, amount, category }),
    });
}

// 감사 로그 API
async function getAuditLogs() {
    return apiCall('/audit/logs');
}

async function getLoginHistory() {
    return apiCall('/audit/login-history');
}

// 유틸리티 함수
function formatCurrency(amount) {
    return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW'
    }).format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleString('ko-KR');
}

// 토큰 관리
function isTokenValid() {
    const token = localStorage.getItem('accessToken');
    if (!token) return false;
    
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.exp * 1000 > Date.now();
    } catch (error) {
        return false;
    }
}

async function ensureValidToken() {
    if (!isTokenValid()) {
        try {
            const response = await refreshToken();
            localStorage.setItem('accessToken', response.accessToken);
            return true;
        } catch (error) {
            // 토큰 갱신 실패 - 로그인 페이지로 이동
            localStorage.clear();
            window.location.href = '/index.html';
            return false;
        }
    }
    return true;
}

// 전역 함수로 노출
window.api = {
    register,
    login,
    logout,
    refreshToken,
    getAccountBalance,
    getTransactionHistory,
    transfer,
    deposit,
    withdraw,
    makePayment,
    getAuditLogs,
    getLoginHistory,
    formatCurrency,
    formatDate,
    isTokenValid,
    ensureValidToken
}; 