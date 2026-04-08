package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnswerReportRepository
        extends JpaRepository<AnswerReport, Long> {

    boolean existsByUserAndAnswer(User user, QuestionAnswer answer);

    long countByAnswer(QuestionAnswer answer);

    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH ar.user u
    """)
    Page<AnswerReport> findAllWithAnswerAndUser(Pageable pageable);

    //상세 조회
    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH ar.user u
        WHERE ar.id = :reportId
    """)
    Optional<AnswerReport> findByIdWithAnswerAndUser(@Param("reportId") Long reportId);
}
