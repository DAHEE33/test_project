package fintech2.easypay.account.repository;

import fintech2.easypay.account.entity.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, Long> {
    
    /**
     * 계좌번호로 가상계좌 조회
     */
    Optional<VirtualAccount> findByAccountNumber(String accountNumber);
    
    /**
     * 사용자 ID로 가상계좌 조회
     */
    Optional<VirtualAccount> findByUserId(Long userId);
    
    /**
     * 계좌번호 중복 체크
     */
    boolean existsByAccountNumber(String accountNumber);
} 