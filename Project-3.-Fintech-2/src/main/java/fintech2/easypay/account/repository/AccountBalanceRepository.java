package fintech2.easypay.account.repository;

import fintech2.easypay.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {
    
    /**
     * Pessimistic Lock을 사용한 계좌 조회
     * 동시성 제어를 위해 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountNumber = :accountNumber")
    Optional<AccountBalance> findByIdWithLock(@Param("accountNumber") String accountNumber);
    
    /**
     * Optimistic Lock을 사용한 계좌 조회 (기본 findById 사용)
     * @Version 필드가 자동으로 처리됨
     */
    // findById는 이미 Optimistic Locking을 지원함 (JPA @Version 사용)
} 