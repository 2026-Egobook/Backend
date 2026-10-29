package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerEmpathy;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerEmpathyRepository extends JpaRepository<AnswerEmpathy, Long> {

    Optional<AnswerEmpathy> findByAnswer_IdAndUser_Id(Long answerId, Long userId);

    long countByAnswer_Id(Long answerId);

    @Query("""
        select e.answer.id as answerId, count(e) as empathyCount
        from AnswerEmpathy e
        where e.answer.id in :answerIds
        group by e.answer.id
    """)
    List<AnswerEmpathyCount> countAllByAnswerIdIn(@Param("answerIds") List<Long> answerIds);

    @Query("""
        select e.answer.id
        from AnswerEmpathy e
        where e.user.id = :userId
          and e.answer.id in :answerIds
    """)
    List<Long> findEmpathizedAnswerIds(
            @Param("userId") Long userId,
            @Param("answerIds") List<Long> answerIds
    );

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AnswerEmpathy e WHERE e.answer.id = :answerId")
    void deleteAllByAnswerId(@Param("answerId") Long answerId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AnswerEmpathy e WHERE e.answer.id IN :answerIds")
    void deleteAllByAnswerIdIn(@Param("answerIds") List<Long> answerIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AnswerEmpathy e WHERE e.user IN :users OR e.answer.id IN (SELECT qa.id FROM QuestionAnswer qa WHERE qa.user IN :users)")
    void bulkDeleteByUserOrAnswerUserIn(@Param("users") List<User> users);

    interface AnswerEmpathyCount {
        Long getAnswerId();
        Long getEmpathyCount();
    }
}
