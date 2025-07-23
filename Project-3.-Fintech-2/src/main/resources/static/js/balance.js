// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    loadBalance();
    loadTransactionHistory();
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

// 잔액 조회
async function loadBalance() {
    const token = localStorage.getItem('accessToken');
    const accountNumber = localStorage.getItem('accountNumber'); // 실제 계좌번호 사용
    
    if (!token || !accountNumber) {
        showAlert('로그인 정보가 없습니다.');
        return;
    }

    try {
        const response = await fetch(`/accounts/${accountNumber}/balance`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            document.getElementById('currentBalance').textContent = 
                `${Number(data.balance).toLocaleString()} 원`;
            document.getElementById('balanceAccountNumber').textContent = data.accountNumber;
        } else {
            // 초기 잔액 0원으로 표시 (계좌가 없는 경우)
            document.getElementById('currentBalance').textContent = '0 원';
            document.getElementById('balanceAccountNumber').textContent = accountNumber;
        }
    } catch (error) {
        console.error('Balance load error:', error);
        showAlert('잔액 조회에 실패했습니다.');
    }
}

// 테스트 입금
async function testDeposit() {
    const amount = document.getElementById('testAmount').value;
    
    if (!amount || amount <= 0) {
        showAlert('올바른 금액을 입력해주세요.');
        return;
    }

    const token = localStorage.getItem('accessToken');
    const accountNumber = localStorage.getItem('accountNumber'); // 실제 계좌번호 사용

    try {
        const response = await fetch('/accounts/update-balance', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                accountNumber: accountNumber,
                amount: Number(amount),
                transactionType: 'DEPOSIT',
                description: '테스트 입금'
            })
        });

        if (response.ok) {
            showAlert('입금이 완료되었습니다!');
            loadBalance(); // 잔액 새로고침
            loadTransactionHistory(); // 거래내역 새로고침
        } else {
            const data = await response.json();
            showAlert(data.message || '입금에 실패했습니다.');
        }
    } catch (error) {
        console.error('Deposit error:', error);
        showAlert('서버 연결에 실패했습니다.');
    }
}

// 테스트 출금
async function testWithdraw() {
    const amount = document.getElementById('testAmount').value;
    
    if (!amount || amount <= 0) {
        showAlert('올바른 금액을 입력해주세요.');
        return;
    }

    const token = localStorage.getItem('accessToken');
    const accountNumber = localStorage.getItem('accountNumber'); // 실제 계좌번호 사용

    try {
        const response = await fetch('/accounts/update-balance', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                accountNumber: accountNumber,
                amount: -Number(amount), // 음수로 전송
                transactionType: 'WITHDRAWAL',
                description: '테스트 출금'
            })
        });

        if (response.ok) {
            showAlert('출금이 완료되었습니다!');
            loadBalance(); // 잔액 새로고침
            loadTransactionHistory(); // 거래내역 새로고침
        } else {
            const data = await response.json();
            showAlert(data.message || '출금에 실패했습니다.');
        }
    } catch (error) {
        console.error('Withdraw error:', error);
        showAlert('서버 연결에 실패했습니다.');
    }
}

// 거래내역 조회
async function loadTransactionHistory() {
    const token = localStorage.getItem('accessToken');
    const accountNumber = localStorage.getItem('accountNumber'); // 실제 계좌번호 사용

    try {
        const response = await fetch(`/accounts/${accountNumber}/transactions`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const transactions = await response.json();
            displayTransactions(transactions);
        } else {
            // 거래내역이 없는 경우
            document.getElementById('transactionList').innerHTML = 
                '<p class="no-data">거래내역이 없습니다.</p>';
        }
    } catch (error) {
        console.error('Transaction history error:', error);
        document.getElementById('transactionList').innerHTML = 
            '<p class="no-data">거래내역을 불러올 수 없습니다.</p>';
    }
}

// 거래내역 표시
function displayTransactions(transactions) {
    const listElement = document.getElementById('transactionList');
    
    if (!transactions || transactions.length === 0) {
        listElement.innerHTML = '<p class="no-data">거래내역이 없습니다.</p>';
        return;
    }

    const html = transactions.map(transaction => {
        const isPositive = transaction.amount > 0;
        const amountClass = isPositive ? 'positive' : 'negative';
        const sign = isPositive ? '+' : '';
        
        return `
            <div class="transaction-item">
                <div class="transaction-info">
                    <div><strong>${transaction.description}</strong></div>
                    <div style="font-size: 12px; color: #666;">
                        ${new Date(transaction.createdAt).toLocaleString()}
                    </div>
                </div>
                <div class="transaction-amount ${amountClass}">
                    ${sign}${Number(transaction.amount).toLocaleString()}원
                </div>
            </div>
        `;
    }).join('');
    
    listElement.innerHTML = html;
}

// 메인으로 이동
function goToMain() {
    window.location.href = '/main.html';
}

// 알림 메시지 표시
function showAlert(message) {
    const alertElement = document.getElementById('balanceAlert');
    if (alertElement) {
        alertElement.textContent = message;
        alertElement.className = 'alert alert-success show';
        
        // 3초 후 자동 숨김
        setTimeout(() => {
            alertElement.classList.remove('show');
        }, 3000);
    }
} 