package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    boolean existsByUserAndQuestion(User user, TodayQuestion question);

    Optional<QuestionAnswer> findByUserAndQuestion(User user, TodayQuestion question);
}
