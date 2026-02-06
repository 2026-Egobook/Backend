package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerReportRepository
        extends JpaRepository<AnswerReport, Long> {

    boolean existsByUserAndAnswer(User user, QuestionAnswer answer);
}
