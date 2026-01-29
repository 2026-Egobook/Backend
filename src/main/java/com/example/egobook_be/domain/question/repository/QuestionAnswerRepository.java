package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.dto.FriendAnswerResDto;
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

    List<QuestionAnswer> findByQuestionAndVisibility(
            TodayQuestion question,
            AnswerVisibility visibility
    );

    @Query("""
        select new com.example.egobook_be.domain.question.dto.FriendAnswerResDto(
            qa.id,
            u.id,
            u.nickname,
            qa.content,
            qa.createdAt
        )
        from QuestionAnswer qa
        join qa.user u
        where qa.question = :question
          and qa.visibility in :visibilities
          and u.id in :friendIds
        order by qa.createdAt desc
    """)
    Slice<FriendAnswerResDto> findFriendsAnswersSlice(
            @Param("question") TodayQuestion question,
            @Param("visibilities") List<AnswerVisibility> visibilities,
            @Param("friendIds") List<Long> friendIds,
            Pageable pageable
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

    @Query("""
        select qa
        from QuestionAnswer qa
        join fetch qa.question q
        where qa.user = :user
        order by qa.createdAt desc
    """)
    Slice<QuestionAnswer> findMyAnswerHistorySlice(
            @Param("user") User user,
            Pageable pageable
    );

    @Query("""
        select qa
        from QuestionAnswer qa
        join fetch qa.question q
        where qa.user.id = :userId
          and q.id = :questionId
    """)
    Optional<QuestionAnswer> findByUserIdAndQuestionIdWithQuestion(
            @Param("userId") Long userId,
            @Param("questionId") Long questionId
    );
}
