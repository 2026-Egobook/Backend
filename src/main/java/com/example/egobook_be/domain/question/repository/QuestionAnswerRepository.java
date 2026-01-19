package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<QuestionAnswer> findByUserOrderByCreatedAtDesc(User user);

    Optional<QuestionAnswer> findByIdAndUser(Long id, User user);

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("""
    select qa
    from QuestionAnswer qa
    join fetch qa.user u
    where qa.question = :question
      and qa.visibility = :visibility
""")
    Slice<QuestionAnswer> findPublicAnswersWithUser(
            @Param("question") TodayQuestion question,
            @Param("visibility") AnswerVisibility visibility,
            Pageable pageable
    );
}
