package com.example.egobook_be.domain.diary.repository;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    boolean existsByUserAndCreatedAtBetween(User user, LocalDateTime startOfToday, LocalDateTime endOfToday);

    boolean existsByUserAndTypeContainingAndCreatedAtBetween(User user, DiaryType diaryType, LocalDateTime startOfToday, LocalDateTime endOfToday);

    boolean existsByUserAndTypeInAndCreatedAtBetween(User user, Set<DiaryType> praise, LocalDateTime startOfToday, LocalDateTime endOfToday);

    List<Diary> findAllByUserAndWrittenAtBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT
        d.date AS date,
        d.emotionLevel AS emotionLevel,
        COUNT(d) AS count,
        MAX(d.writtenAt) AS latest
    FROM Diary d
    WHERE d.user = :user
      AND d.emotionLevel IS NOT NULL
      AND d.date BETWEEN :start AND :end
    GROUP BY d.date, d.emotionLevel
    ORDER BY d.date, count DESC, latest DESC
    """)
    List<DailyEmotionCount> findDailyEmotions(User user, LocalDate start, LocalDate end);

    //  날짜별 일간 칭찬서 발송 대상자 수 집계 (관리자 API용)
    /**
     * 날짜별로 그날 일기를 작성했고 daily_praise=true인 유저 수 집계
     * - 관리자 API에서 "실제 일간 칭찬서 발송 대상자 수(scheduledCount)"를 계산하기 위해 사용
     * @param startDate : 조회 시작일
     * @param endDate : 조회 종료일
     * @return : [날짜, 발송 대상 유저 수] 리스트
     */
    @Query("""
        SELECT d.date, COUNT(DISTINCT d.user.id)
        FROM Diary d
        WHERE d.date BETWEEN :startDate AND :endDate
          AND d.user.dailyPraise = true
        GROUP BY d.date
        ORDER BY d.date ASC
    """)
    List<Object[]> countDailyPraiseTargetsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT DISTINCT d
    FROM Diary d
    WHERE d.user = :user
      AND d.date = :date
      AND (:type IS NULL OR :type MEMBER OF d.type)
    ORDER BY d.writtenAt DESC
""")
    Slice<Diary> findAllByUserAndTypeAndDate(User user, DiaryType type, LocalDate date, Pageable pageable);

    int countByUserAndDate(User user, LocalDate date);

    List<Diary> findAllByUserIdAndWrittenAtAfter(Long userId, LocalDateTime writtenAt);

    Long countByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("""
    SELECT dt as type, COUNT(d) as count
    FROM Diary d
    JOIN d.type dt
    WHERE d.date >= :start
      AND d.date <= :end
    GROUP BY dt
    """)
    List<DiaryTypeCount> countByTypeAndDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    interface DiaryTypeCount {
        DiaryType getType();
        Long getCount();
    }

    interface DailyEmotionCount {
        LocalDate getDate();
        Integer getEmotionLevel();
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Diary d WHERE d.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);

    @Query("SELECT AVG(d.emotionLevel) FROM Diary d WHERE d.user.id = :userId AND d.writtenAt BETWEEN :start AND :end AND d.emotionLevel > 0")
    Double findAvgEmotionLevel(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 특정 날짜의 일기 목록 조회 (일간용)
    List<Diary> findByUserIdAndDate(Long userId, LocalDate date);

    // 특정 기간 사이의 일기 목록 조회 (주간용)
    List<Diary> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate start, LocalDate end);

    long countByUser(User user);
}
