package fintech2.easypay.auth.repository;

import fintech2.easypay.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 휴대폰 번호로 사용자 조회 (VirtualAccount 포함)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.virtualAccount WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    /**
     * 휴대폰 번호 중복 체크
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * User와 VirtualAccount 함께 조회
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.virtualAccount WHERE u.id = :id")
    Optional<User> findUserWithAccount(@Param("id") Long id);
} 