// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    loadAlarms();
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

// 알람 목록 로드
async function loadAlarms() {
    const token = localStorage.getItem('accessToken');
    
    try {
        // 실제로는 서버에서 알람 데이터를 가져와야 하지만,
        // 현재는 로컬 스토리지의 감사 로그를 시뮬레이션
        const mockAlarms = generateMockAlarms();
        displayAlarms(mockAlarms);
    } catch (error) {
        console.error('Alarm load error:', error);
        showAlert('알람을 불러올 수 없습니다.');
    }
}

// 모의 알람 데이터 생성
function generateMockAlarms() {
    const alarms = [];
    const now = new Date();
    
    // 잔액 변동 알람
    alarms.push({
        id: 1,
        type: 'BALANCE_CHANGE',
        category: 'balance',
        message: '계좌 VA12345678의 잔액이 입금되었습니다. 금액: 50,000원, 잔액: 50,000원',
        timestamp: new Date(now.getTime() - 5 * 60 * 1000), // 5분 전
        level: 'info'
    });
    
    // 잔액 부족 알람
    alarms.push({
        id: 2,
        type: 'INSUFFICIENT_BALANCE',
        category: 'balance',
        message: '계좌 VA12345678의 잔액이 부족합니다. 현재 잔액: 30,000원, 필요 금액: 100,000원',
        timestamp: new Date(now.getTime() - 10 * 60 * 1000), // 10분 전
        level: 'warning'
    });
    
    // 로그인 실패 알람
    alarms.push({
        id: 3,
        type: 'LOGIN_FAILURE',
        category: 'login',
        message: '로그인 실패. 사용자: 홍길동, 휴대폰: 010-1234-5678, 사유: 비밀번호 불일치',
        timestamp: new Date(now.getTime() - 15 * 60 * 1000), // 15분 전
        level: 'warning'
    });
    
    // 계정 잠금 알람
    alarms.push({
        id: 4,
        type: 'ACCOUNT_LOCK',
        category: 'login',
        message: '계정이 잠겼습니다. 사용자: 홍길동, 휴대폰: 010-1234-5678, 사유: 5회 연속 로그인 실패',
        timestamp: new Date(now.getTime() - 20 * 60 * 1000), // 20분 전
        level: 'error'
    });
    
    // 시스템 알람
    alarms.push({
        id: 5,
        type: 'SYSTEM_ERROR',
        category: 'system',
        message: '데이터베이스 연결 오류가 발생했습니다.',
        timestamp: new Date(now.getTime() - 30 * 60 * 1000), // 30분 전
        level: 'error'
    });
    
    return alarms.sort((a, b) => b.timestamp - a.timestamp); // 최신순 정렬
}

// 알람 표시
function displayAlarms(alarms) {
    const listElement = document.getElementById('alarmList');
    
    if (!alarms || alarms.length === 0) {
        listElement.innerHTML = '<div class="no-alarms">알람이 없습니다.</div>';
        return;
    }

    const html = alarms.map(alarm => {
        const levelClass = getLevelClass(alarm.level);
        const typeIcon = getTypeIcon(alarm.type);
        
        return `
            <div class="alarm-item ${levelClass}">
                <div class="alarm-header">
                    <div class="alarm-type">${typeIcon} ${getTypeName(alarm.type)}</div>
                    <div class="alarm-time">${formatTime(alarm.timestamp)}</div>
                </div>
                <div class="alarm-message">${alarm.message}</div>
            </div>
        `;
    }).join('');
    
    listElement.innerHTML = html;
}

// 알람 필터링
function filterAlarms(category) {
    // 필터 버튼 활성화 상태 변경
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 실제로는 서버에서 필터링된 데이터를 가져와야 함
    const mockAlarms = generateMockAlarms();
    const filteredAlarms = category === 'all' 
        ? mockAlarms 
        : mockAlarms.filter(alarm => alarm.category === category);
    
    displayAlarms(filteredAlarms);
}

// 레벨별 CSS 클래스
function getLevelClass(level) {
    switch (level) {
        case 'error': return 'error';
        case 'warning': return 'warning';
        case 'info': return 'success';
        default: return '';
    }
}

// 타입별 아이콘
function getTypeIcon(type) {
    switch (type) {
        case 'BALANCE_CHANGE': return '💰';
        case 'INSUFFICIENT_BALANCE': return '⚠️';
        case 'LOGIN_FAILURE': return '🔐';
        case 'ACCOUNT_LOCK': return '🚫';
        case 'SYSTEM_ERROR': return '💥';
        default: return '🔔';
    }
}

// 타입별 이름
function getTypeName(type) {
    switch (type) {
        case 'BALANCE_CHANGE': return '잔액 변동';
        case 'INSUFFICIENT_BALANCE': return '잔액 부족';
        case 'LOGIN_FAILURE': return '로그인 실패';
        case 'ACCOUNT_LOCK': return '계정 잠금';
        case 'SYSTEM_ERROR': return '시스템 오류';
        default: return '알람';
    }
}

// 시간 포맷팅
function formatTime(timestamp) {
    const now = new Date();
    const diff = now - timestamp;
    
    if (diff < 60 * 1000) return '방금 전';
    if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}분 전`;
    if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}시간 전`;
    return timestamp.toLocaleDateString();
}

// 메인으로 이동
function goToMain() {
    window.location.href = '/main.html';
}

// 알림 메시지 표시
function showAlert(message) {
    const alertElement = document.getElementById('alarmAlert');
    if (alertElement) {
        alertElement.textContent = message;
        alertElement.className = 'alert alert-success show';
        
        // 3초 후 자동 숨김
        setTimeout(() => {
            alertElement.classList.remove('show');
        }, 3000);
    }
} 