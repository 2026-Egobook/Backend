package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnswerReportRepository
        extends JpaRepository<AnswerReport, Long>, AnswerReportRepositoryCustom{

    boolean existsByUserAndAnswer(User user, QuestionAnswer answer);

    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH ar.user u
    """)
    Page<AnswerReport> findAllWithAnswerAndUser(Pageable pageable);
}
