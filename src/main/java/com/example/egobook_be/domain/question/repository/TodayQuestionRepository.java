package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.TodayQuestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TodayQuestionRepository extends JpaRepository<TodayQuestion, Long> {
    Optional<TodayQuestion> findByQuestionDate(LocalDate date);

    // 해당 날짜에 삭제되지 않은 질문이 있는지 확인
    boolean existsByQuestionDateAndDeletedAtIsNull(LocalDate questionDate);

    // 삭제되지 않은 질문들만 페이징 조회
    Slice<TodayQuestion> findAllByDeletedAtIsNull(Pageable pageable);
}
