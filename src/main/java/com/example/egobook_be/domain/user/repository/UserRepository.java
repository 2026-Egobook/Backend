package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
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
    List<Long> findReceivableUsers(OffsetDateTime now, PageRequest pageable);


    @Query("""
    select u.id
    from User u
    where (u.letterReceiveBlockedUntil is null or u.letterReceiveBlockedUntil <= :now)
""")
    List<Long> findAvailableReceivers(@Param("now") OffsetDateTime now, Pageable pageable);

    Optional<User> findByEmail(String email);
}
