package fintech2.easypay.auth.repository;

import fintech2.easypay.auth.entity.LoginHistory;
import fintech2.easypay.common.LoginResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    /**
     * 사용자별 로그인 이력 조회
     */
    Page<LoginHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 휴대폰 번호별 로그인 이력 조회
     */
    Page<LoginHistory> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber, Pageable pageable);
    
    /**
     * 특정 기간 동안의 로그인 이력 조회
     */
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.createdAt BETWEEN :startDate AND :endDate")
    List<LoginHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * 실패한 로그인 시도 조회
     */
    List<LoginHistory> findByResultAndPhoneNumberOrderByCreatedAtDesc(LoginResult result, String phoneNumber);
    
    /**
     * IP 주소별 로그인 이력 조회 (보안 분석용)
     */
    List<LoginHistory> findByIpAddressOrderByCreatedAtDesc(String ipAddress);
} 