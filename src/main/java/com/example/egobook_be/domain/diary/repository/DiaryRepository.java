package com.example.egobook_be.domain.diary.repository;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    int countByUserAndWrittenAtBetween(User user, LocalDateTime writtenAtAfter, LocalDateTime writtenAtBefore);

    boolean existsByUserAndCreatedAtBetween(User user, LocalDateTime startOfToday, LocalDateTime endOfToday);

    boolean existsByUserAndTypeContainingAndCreatedAtBetween(User user, DiaryType diaryType, LocalDateTime startOfToday, LocalDateTime endOfToday);

    boolean existsByUserAndTypeInAndCreatedAtBetween(User user, Set<DiaryType> praise, LocalDateTime startOfToday, LocalDateTime endOfToday);

    @Query("""
    SELECT d
    FROM Diary d 
    WHERE d.user = :user 
      AND d.writtenAt BETWEEN :start AND :end\n
      AND (:type IS NULL OR :type MEMBER OF d.type) ORDER BY d.writtenAt DESC
    """)
    Slice<Diary> findAllByUserAndTypeAndWrittenAtBetween(
            User user, DiaryType type, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    @Query("""
    SELECT
        DATE(d.writtenAt) AS date,
        d.emotionLevel AS emotionLevel,
        COUNT(d) AS count,
        MAX(d.writtenAt) AS latest
    FROM Diary d
    WHERE d.user = :user
      AND d.emotionLevel IS NOT NULL
      AND d.writtenAt BETWEEN :start AND :end
    GROUP BY DATE(d.writtenAt), d.emotionLevel
    ORDER BY DATE(d.writtenAt), count DESC, latest DESC
    """)
    List<DailyEmotionCount> findDailyEmotions(User user, LocalDateTime start, LocalDateTime end);

    interface DailyEmotionCount {
        LocalDate getDate();
        Integer getEmotionLevel();
        Long getCount();
        LocalDateTime getLatest();
    }
}
