package fintech2.easypay.account.repository;

import fintech2.easypay.account.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    List<TransactionHistory> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
} 