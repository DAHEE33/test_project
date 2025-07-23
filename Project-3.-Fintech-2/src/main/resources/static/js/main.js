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
        alert('로그인이 필요합니다.');
        window.location.href = '/index.html';
        return;
    }
}

// 사용자 정보 로드
function loadUserInfo() {
    const userPhone = localStorage.getItem('userPhone');
    if (userPhone) {
        // 휴대폰 번호에서 이름 추출 (실제로는 서버에서 가져와야 함)
        const userName = `${userPhone.substring(0, 3)}****${userPhone.substring(9)}`;
        document.getElementById('userName').textContent = userName;
        
        // 계좌번호도 표시 (실제로는 서버에서 가져와야 함)
        const accountNumber = localStorage.getItem('accountNumber') || 'VA12345678';
        document.getElementById('accountNumber').textContent = accountNumber;
    }
}

// 알림 확인
function loadNotifications() {
    // 실제로는 서버에서 알림 개수를 가져와야 함
    const notificationCount = Math.floor(Math.random() * 3); // 테스트용 랜덤
    
    if (notificationCount > 0) {
        const badge = document.getElementById('notificationBadge');
        badge.textContent = notificationCount;
        badge.style.display = 'flex';
    }
}

// 알림 체크
function checkNotifications() {
    alert('새로운 알림이 없습니다.');
    
    // 알림 배지 숨기기
    const badge = document.getElementById('notificationBadge');
    badge.style.display = 'none';
}

// 서비스 버튼 클릭 핸들러들
function goToTransfer() {
    alert('송금 기능은 추후 구현 예정입니다.\n현재는 잔액조회만 가능합니다.');
}

function goToPayment() {
    alert('결제 기능은 추후 구현 예정입니다.\n현재는 잔액조회만 가능합니다.');
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
    localStorage.removeItem('userPhone');
    localStorage.removeItem('accountNumber');
    window.location.href = '/index.html';
} 