// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    // 로그인 폼 처리
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    // 회원가입 폼 처리
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
});

// 로그인 처리
async function handleLogin(e) {
    e.preventDefault();
    
    const phoneNumber = document.getElementById('phoneNumber').value;
    const password = document.getElementById('password').value;
    const loading = document.getElementById('loginLoading');
    
    if (!phoneNumber || !password) {
        showAlert('loginAlert', '휴대폰 번호와 비밀번호를 입력해주세요.', 'error');
        return;
    }

    loading.classList.add('show');
    
    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                phoneNumber: phoneNumber,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            // 토큰 저장
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('userPhone', phoneNumber);
            
            // 계좌번호가 응답에 포함되어 있다면 저장
            if (data.accountNumber) {
                localStorage.setItem('accountNumber', data.accountNumber);
            }
            
            showAlert('loginAlert', '로그인 성공!', 'success');
            setTimeout(() => {
                window.location.href = '/main.html';
            }, 1000);
        } else {
            showAlert('loginAlert', data.message || '로그인에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showAlert('loginAlert', '서버 연결에 실패했습니다.', 'error');
    } finally {
        loading.classList.remove('show');
    }
}

// 회원가입 처리
async function handleRegister(e) {
    e.preventDefault();
    
    const phoneNumber = document.getElementById('regPhoneNumber').value;
    const password = document.getElementById('regPassword').value;
    const name = document.getElementById('regName').value;
    const loading = document.getElementById('registerLoading');
    
    if (!phoneNumber || !password || !name) {
        showAlert('registerAlert', '모든 항목을 입력해주세요.', 'error');
        return;
    }

    // 휴대폰 번호 형식 검증
    const phonePattern = /^010-\d{4}-\d{4}$/;
    if (!phonePattern.test(phoneNumber)) {
        showAlert('registerAlert', '휴대폰 번호 형식이 올바르지 않습니다. (010-1234-5678)', 'error');
        return;
    }

    // 비밀번호 길이 검증
    if (password.length < 6) {
        showAlert('registerAlert', '비밀번호는 6자 이상이어야 합니다.', 'error');
        return;
    }

    loading.classList.add('show');
    
    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                phoneNumber: phoneNumber,
                password: password,
                name: name
            })
        });

        const data = await response.json();

        if (response.ok) {
            // 회원가입 완료 팝업
            alert('회원가입이 완료되었습니다!');
            
            // 계좌번호가 응답에 포함되어 있다면 저장
            if (data.accountNumber) {
                localStorage.setItem('accountNumber', data.accountNumber);
            }
            
            // 로그인 페이지로 이동
            window.location.href = '/index.html';
        } else {
            showAlert('registerAlert', data.message || '회원가입에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Register error:', error);
        showAlert('registerAlert', '서버 연결에 실패했습니다.', 'error');
    } finally {
        loading.classList.remove('show');
    }
}

// 페이지 이동 함수들
function goToRegister() {
    window.location.href = '/register.html';
}

function goToLogin() {
    window.location.href = '/index.html';
}

// 알림 메시지 표시
function showAlert(alertId, message, type) {
    const alertElement = document.getElementById(alertId);
    if (alertElement) {
        alertElement.textContent = message;
        alertElement.className = `alert alert-${type} show`;
        
        // 3초 후 자동 숨김
        setTimeout(() => {
            alertElement.classList.remove('show');
        }, 3000);
    }
} 