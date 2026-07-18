package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.dto.FriendAnswerResDto;
import com.example.egobook_be.domain.question.dto.PublicAnswerResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    boolean existsByUserAndQuestion(User user, TodayQuestion question);

    long countByUser(User user);

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
            u.level,
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

    boolean existsByUserAndCreatedAtBetween(User user, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);

    /**
     * 기간 내 질문에 달린 PUBLIC 답변을 질문 날짜 내림차순, 답변 작성일 내림차순으로 조회한다.
     * @param startDate : 조회 시작일 (질문 날짜 기준)
     * @param endDate : 조회 종료일 (질문 날짜 기준)
     * @param visibility : 조회할 공개 범위 (PUBLIC 고정 사용)
     * @return : 질문+유저가 함께 fetch된 QuestionAnswer 목록
     */
    @Query("""
        select qa
        from QuestionAnswer qa
        join fetch qa.user u
        join fetch qa.question q
        where q.questionDate between :startDate and :endDate
          and q.deletedAt is null
          and qa.visibility = :visibility
        order by q.questionDate desc, qa.createdAt desc
    """)
    List<QuestionAnswer> findAllByQuestionDateBetweenAndVisibility(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("visibility") AnswerVisibility visibility
    );

}
