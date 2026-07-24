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

    /**
     * 기간 내 삭제되지 않은 질문을 날짜 내림차순으로 페이징 조회한다.
     * @param startDate : 조회 시작일
     * @param endDate : 조회 종료일
     * @param pageable : 페이징 정보
     * @return : Slice<TodayQuestion>
     */
    Slice<TodayQuestion> findAllByQuestionDateBetweenAndDeletedAtIsNullOrderByQuestionDateDesc(
            LocalDate startDate, LocalDate endDate, Pageable pageable
    );
}
