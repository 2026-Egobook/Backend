package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerReportRepository
        extends JpaRepository<AnswerReport, Long>, AnswerReportRepositoryCustom{

    boolean existsByUserAndAnswer(User user, QuestionAnswer answer);

    long countByAnswer(QuestionAnswer answer);

    // 승인(RESOLVED) 처리된 신고도 목록에 그대로 노출한다 (반려는 DB에서도 삭제되어 자연히 목록에서 빠짐)
    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH ar.user u
        ORDER BY ar.createdAt DESC, ar.id DESC
    """)
    Page<AnswerReport> findAllWithAnswerAndUser(Pageable pageable);

    //상세 조회 (reportId 단건 - 승인/반려 처리용)
    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH ar.user u
        WHERE ar.id = :reportId
    """)
    Optional<AnswerReport> findByIdWithAnswerAndUser(@Param("reportId") Long reportId);

    // 신고 상세보기: 신고된 컨텐츠(answerId) 기준으로 해당 컨텐츠에 달린 모든 신고 내역을 조회
    @Query("""
        SELECT ar
        FROM AnswerReport ar
        JOIN FETCH ar.answer a
        JOIN FETCH a.user
        JOIN FETCH ar.user u
        WHERE ar.answer.id = :answerId
        ORDER BY ar.createdAt DESC
    """)
    List<AnswerReport> findAllByAnswerId(@Param("answerId") Long answerId);

    @Query("SELECT COUNT(ar) FROM AnswerReport ar WHERE ar.answer.id = :answerId")
    long countByAnswerId(@Param("answerId") Long answerId);

    //수동 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AnswerReport ar WHERE ar.answer.id = :answerId")
    void deleteAllByAnswerId(@Param("answerId") Long answerId);

    //승인 획수 카운트
    @Query("SELECT COUNT(ar) FROM AnswerReport ar WHERE ar.answer.id = :answerId AND ar.status = :status")
    long countByAnswerIdAndStatus(@Param("answerId") Long answerId, @Param("status") ReportStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AnswerReport ar WHERE ar.user IN :users OR ar.answer.id IN (SELECT qa.id FROM QuestionAnswer qa WHERE qa.user IN :users)")
    void bulkDeleteByUserOrAnswerUserIn(@Param("users") List<User> users);
}
