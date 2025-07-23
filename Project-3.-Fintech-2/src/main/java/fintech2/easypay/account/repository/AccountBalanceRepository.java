package fintech2.easypay.account.repository;

import fintech2.easypay.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {} 