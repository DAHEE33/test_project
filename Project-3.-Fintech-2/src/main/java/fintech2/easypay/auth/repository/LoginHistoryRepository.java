package fintech2.easypay.auth.repository;

import fintech2.easypay.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByPhoneNumberOrderByLoginAtDesc(String phoneNumber);
    List<LoginHistory> findByUserIdOrderByLoginAtDesc(Long userId);
} 