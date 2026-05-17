package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,UserRepositoryCustom {

    /**
     * 신규 사용자 가입 시, AccountCode를 만들어 지정해줄 때 해당 AccountCode가 이미 존재하는지 확인하기 위한 함수 
     * @param accountCode
     * @return
     */
    boolean existsByAccountCode(String accountCode);

    /**
     * 신규 사용자 가입 시, Nickname을 랜덤으로 만들어 지정해줄 때 해당 닉네임이 이미 존재하는지 확인하기 위한 함수
     * @param nickname
     * @return
     */
    boolean existsByNickname(String nickname);

    List<User> findByNicknameContainingIgnoreCaseOrAccountCodeContainingIgnoreCase(
            String nickname,
            String accountCode
    );

    @Modifying(clearAutomatically = true)
    @Query("update User u set " +
            "u.isFirstAttendanceToday = true " +
            "where u.isFirstAttendanceToday = false")
    void resetAllAttendancesStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);

    /** 삭제 대상 유저 조회 (PurgeAt이 현재 시간보다 과거인 경우) */
    List<User> findByStatusAndPurgeAtBefore(UserStatus status, LocalDateTime now);

    // 수신에 동의한 유저만 조회
    List<User> findByDailyPraiseTrue();
    List<User> findAllByWeeklyAnalysisEnabledTrue();


    // 편지 수신 가능으로 변한 유저 찾기
    @Query("""
    select u.id
    from User u
    where u.letterReceiveBlockedUntil is not null
      and u.letterReceiveBlockedUntil <= :now
""")
    List<Long> findReceivableUsers(LocalDateTime now, PageRequest pageable);


    @Query("""
    select u.id
    from User u
    where (u.letterReceiveBlockedUntil is null or u.letterReceiveBlockedUntil <= :now)
""")
    List<Long> findAvailableReceivers(@Param("now") LocalDateTime now, Pageable pageable);

    Optional<User> findByEmail(String email);

    @Query("select new com.example.egobook_be.domain.user.dto.SearchUserResDto(" +
            "u.id, u.accountCode, u.email, u.nickname, u.status, u.lastLoginAt, u.createdAt)" +
            "from User u " +
            "where (u.nickname like concat('%', :keyword, '%') or " +
            "       u.email like concat('%', :keyword, '%') or " +
            "       u.accountCode like concat('%', :keyword, '%')) " +
            "and (:status is null or u.status = :status)")
    Slice<SearchUserResDto> findUsersByKeywordAndStatus(@Param("keyword")String keyword, @Param("status")UserStatus status, Pageable pageable);

    @Query("select new com.example.egobook_be.domain.user.dto.SearchUserResDto(" +
            "u.id, u.accountCode, u.email, u.nickname, u.status, u.lastLoginAt, u.createdAt)" +
            "from User u " +
            "where (:status is null or u.status = :status)")
    Slice<SearchUserResDto> findUsersByStatus(@Param("status") UserStatus status, Pageable pageable);

    Long countByCreatedAtBefore(LocalDateTime createdAtBefore);
}
