package fintech2.easypay.account.repository;

import fintech2.easypay.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {
    
    /**
     * 계좌 잔액 조회 (비관적 락)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId = :accountId")
    Optional<AccountBalance> findByAccountIdWithLock(@Param("accountId") Long accountId);
    
    /**
     * 계좌 잔액 조회 (읽기 전용)
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId = :accountId")
    Optional<AccountBalance> findByAccountId(@Param("accountId") Long accountId);
} 