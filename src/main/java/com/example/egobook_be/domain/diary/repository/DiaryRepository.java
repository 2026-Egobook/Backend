package com.example.egobook_be.domain.diary.repository;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("""
    SELECT d 
    FROM Diary d 
    JOIN d.type t 
    WHERE d.user = :user 
      AND d.date = :date 
      AND (:type IS NULL OR :type MEMBER OF d.type)
    ORDER BY d.writtenAt DESC
""")
    Slice<Diary> findAllByUserAndTypeAndDate(User user, DiaryType type, LocalDate date, Pageable pageable);

    int countByUserAndDate(User user, LocalDate date);

    List<Diary> findAllByUserIdAndWrittenAtAfter(Long userId, LocalDateTime writtenAt);

    interface DailyEmotionCount {
        LocalDate getDate();
        Integer getEmotionLevel();
    }

    @Query("SELECT AVG(d.emotionLevel) FROM Diary d WHERE d.user.id = :userId AND d.writtenAt BETWEEN :start AND :end AND d.emotionLevel > 0")
    Double findAvgEmotionLevel(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 특정 날짜의 일기 목록 조회 (일간용)
    List<Diary> findByUserIdAndDate(Long userId, LocalDate date);

    // 특정 기간 사이의 일기 목록 조회 (주간용)
    List<Diary> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate start, LocalDate end);
}
