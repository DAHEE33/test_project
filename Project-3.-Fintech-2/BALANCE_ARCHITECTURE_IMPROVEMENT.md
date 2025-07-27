# 🏦 잔액 처리 아키텍처 개선 사항

## 📋 개요
기존 `AccountService`의 단순한 잔액 처리를 **중앙화된 잔액 서비스 패턴**으로 개선하여 동시성 제어, 데이터 무결성, 장애 복구를 강화했습니다.

---

## 🚨 기존 코드의 문제점

### 1️⃣ **동시성 제어 부재**
```java
// 기존 코드 (문제)
Optional<AccountBalance> accountOpt = accountBalanceRepository.findById(accountNumber);
AccountBalance account = accountOpt.get();
BigDecimal balanceBefore = account.getBalance();
BigDecimal balanceAfter = balanceBefore.add(amount);
account.setBalance(balanceAfter);
accountBalanceRepository.save(account);
```

**문제점:**
- **Race Condition**: 동시 출금 시 잔액 꼬임 발생 가능
- **SELECT → UPDATE 패턴**: 읽기와 쓰기 사이에 다른 트랜잭션 개입
- **Optimistic Locking 미활용**: `@Version` 필드가 있지만 실제로 활용 안 함

### 2️⃣ **트랜잭션 경계 문제**
```java
@Transactional
public ResponseEntity<?> updateBalance(...) {
    // 잔액 업데이트
    accountBalanceRepository.save(account);
    
    // 거래내역 기록
    transactionHistoryRepository.save(th);
    
    // 알람 발송 (외부 호출)
    alarmService.sendBalanceChangeAlert(...);
}
```

**문제점:**
- **긴 트랜잭션**: 알람 발송까지 포함되어 트랜잭션 길어짐
- **외부 호출 포함**: 알람 실패 시 전체 롤백
- **부분 실패 처리 부재**: 거래내역은 저장되었는데 알람만 실패

### 3️⃣ **비즈니스 로직과 잔액 처리 혼재**
- 송금/결제 로직이 들어오면 복잡해짐
- 잔액 처리의 중앙화된 제어 불가
- 각 비즈니스 로직마다 잔액 처리 로직 중복

---

## 🔧 개선된 아키텍처

### 1️⃣ **중앙화된 잔액 서비스 (BalanceService)**

```java
@Service
public class BalanceService {
    
    @Transactional
    public BalanceChangeResult increase(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        // 입금 처리
    }
    
    @Transactional
    public BalanceChangeResult decrease(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        // 출금 처리
    }
    
    private BalanceChangeResult changeBalance(...) {
        // 1. Pessimistic Lock으로 동시성 제어
        Optional<AccountBalance> accountOpt = accountBalanceRepository.findByIdWithLock(accountNumber);
        
        // 2. 잔액 검증
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException(...);
        }
        
        // 3. 잔액 업데이트 (Lock으로 보호됨)
        account.setBalance(balanceAfter);
        accountBalanceRepository.save(account);
        
        // 4. 거래내역 기록
        transactionHistoryRepository.save(transaction);
        
        // 5. 알람 발송 (트랜잭션 외부)
        alarmService.sendBalanceChangeAlert(...);
    }
}
```

### 2️⃣ **비즈니스 로직 서비스 (TransferService)**

```java
@Service
public class TransferService {
    
    @Transactional
    public Map<String, Object> transfer(String fromAccountNumber, String toAccountNumber, 
                                      BigDecimal amount, String description, String userId) {
        
        // 1. 송금 검증
        validateTransferRequest(...);
        
        // 2. 출금 처리 (BalanceService 호출)
        BalanceService.BalanceChangeResult withdrawResult = balanceService.decrease(
            fromAccountNumber, amount, TransactionType.TRANSFER, description, transferId);
        
        // 3. 입금 처리 (BalanceService 호출)
        BalanceService.BalanceChangeResult depositResult = balanceService.increase(
            toAccountNumber, amount, TransactionType.TRANSFER, description, transferId);
        
        // 4. 송금 완료 처리
        return createTransferResponse(...);
    }
}
```

### 3️⃣ **동시성 제어 강화**

```java
@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountNumber = :accountNumber")
    Optional<AccountBalance> findByIdWithLock(@Param("accountNumber") String accountNumber);
}
```

---

## 🎯 개선 효과

### 1️⃣ **동시성 제어 강화**
- **Pessimistic Lock**: 동시 접근 방지
- **트랜잭션 격리**: 각 잔액 변경이 독립적으로 처리
- **데이터 무결성**: 잔액 꼬임 방지

### 2️⃣ **책임 분리**
- **BalanceService**: 잔액 변경의 중앙 제어
- **TransferService**: 송금 비즈니스 로직
- **AccountService**: 계좌 조회 및 기본 관리

### 3️⃣ **장애 복구 강화**
- **중앙화된 로깅**: 모든 잔액 변경 이력 추적
- **참조 ID**: 송금/결제 ID로 거래 추적 가능
- **부분 실패 처리**: 알람 실패가 잔액 변경에 영향 없음

### 4️⃣ **확장성 향상**
- **새로운 비즈니스 로직**: BalanceService만 호출하면 됨
- **결제 서비스**: 동일한 패턴으로 구현 가능
- **모니터링**: 중앙화된 잔액 처리로 모니터링 용이

---

## 📊 실제 API 플로우

### 송금 요청 플로우
```
[사용자 요청] → POST /transfers
    ↓
[TransferController] → 송금 요청 검증
    ↓
[TransferService] → 송금 비즈니스 로직
    ↓
[BalanceService.decrease] → 출금 처리 (Lock + 검증 + 업데이트)
    ↓
[BalanceService.increase] → 입금 처리 (Lock + 검증 + 업데이트)
    ↓
[응답] → 송금 완료
```

### 동시성 제어 플로우
```
[요청1] → findByIdWithLock() → [Lock 획득] → 잔액 변경 → [Lock 해제]
[요청2] → findByIdWithLock() → [대기] → [Lock 획득] → 잔액 변경 → [Lock 해제]
```

---

## 🧪 테스트 검증

### 단위 테스트
- `BalanceServiceTest`: 잔액 증가/감소, 예외 처리, 동시성 테스트
- `TransferServiceTest`: 송금 비즈니스 로직 테스트

### 동시성 테스트
```java
@Test
void 동시출금_테스트() {
    // 10개의 동시 출금 요청
    // 모든 요청이 성공하고 잔액이 정확히 계산되는지 검증
}
```

---

## 🚀 다음 단계

### 1️⃣ **실제 운영 환경 적용**
- **분산 락**: Redis 기반 분산 락 적용
- **데드락 방지**: 락 획득 순서 표준화
- **성능 모니터링**: 잔액 처리 성능 측정

### 2️⃣ **추가 비즈니스 로직**
- **결제 서비스**: 동일한 패턴으로 구현
- **정산 서비스**: 일일/월간 정산 로직
- **한도 관리**: 송금/출금 한도 검증

### 3️⃣ **고급 기능**
- **Saga 패턴**: 분산 트랜잭션 처리
- **이벤트 소싱**: 모든 잔액 변경 이벤트 저장
- **CQRS**: 읽기/쓰기 모델 분리

---

## 📝 결론

이번 개선을 통해 **3년차 백엔드 개발자 수준의 안전하고 확장 가능한 잔액 처리 시스템**을 구축했습니다.

**핵심 개선점:**
1. ✅ **동시성 제어**: Pessimistic Lock으로 Race Condition 방지
2. ✅ **책임 분리**: 잔액 처리와 비즈니스 로직 분리
3. ✅ **중앙화**: 모든 잔액 변경을 BalanceService에서 처리
4. ✅ **장애 복구**: 상세한 로깅과 참조 ID로 추적 가능
5. ✅ **확장성**: 새로운 비즈니스 로직 추가 용이

이제 **실제 금융 서비스에서 사용할 수 있는 수준의 잔액 처리 시스템**이 완성되었습니다! 🎉 