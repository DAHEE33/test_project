// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    loadUserInfo();
    loadNotifications();
});

// 인증 확인
function checkAuth() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/index.html';
    }
}

// 사용자 정보 로드
async function loadUserInfo() {
    const token = localStorage.getItem('accessToken');
    const accountNumber = localStorage.getItem('accountNumber');
    
    if (accountNumber) {
        document.getElementById('accountNumber').textContent = accountNumber;
    }
    
    // 사용자 이름 설정 (실제로는 서버에서 가져와야 함)
    const userName = localStorage.getItem('userName') || '고객님';
    document.getElementById('userName').textContent = userName;
}

// 알림 개수 로드
async function loadNotifications() {
    const token = localStorage.getItem('accessToken');
    
    if (!token) {
        return;
    }
    
    try {
        const response = await fetch('/api/alarms/count', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            const count = data.count || 0;
            
            const badge = document.getElementById('notificationBadge');
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Notification count load error:', error);
        // 에러 시 배지 숨김
        const badge = document.getElementById('notificationBadge');
        badge.style.display = 'none';
    }
}

// 알림 체크
function checkNotifications() {
    alert('새로운 알림이 없습니다.');
    
    // 알림 배지 숨기기
    const badge = document.getElementById('notificationBadge');
    badge.style.display = 'none';
}

// 서비스 페이지 이동
function goToTransfer() {
    alert('송금 서비스는 준비 중입니다.');
}

function goToPayment() {
    alert('결제 서비스는 준비 중입니다.');
}

function goToBalance() {
    window.location.href = '/balance.html';
}

// 알람 페이지로 이동
function goToAlarm() {
    window.location.href = '/alarm.html';
}

// 로그아웃
function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('accountNumber');
    localStorage.removeItem('userName');
    window.location.href = '/index.html';
} 