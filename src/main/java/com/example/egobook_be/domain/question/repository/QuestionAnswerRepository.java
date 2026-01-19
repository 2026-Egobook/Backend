package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    boolean existsByUserAndQuestion(User user, TodayQuestion question);

    Optional<QuestionAnswer> findByUserAndQuestion(User user, TodayQuestion question);

//    boolean existsByUserAndQuestion(
//            com.example.egobook_be.domain.user.entity.User user,
//            TodayQuestion question
//    );

    List<QuestionAnswer> findByQuestionAndVisibility(
            TodayQuestion question,
            AnswerVisibility visibility
    );

    List<QuestionAnswer> findByQuestionAndVisibilityAndUserIn(
            TodayQuestion question,
            AnswerVisibility visibility,
            List<User> users
    );
}
