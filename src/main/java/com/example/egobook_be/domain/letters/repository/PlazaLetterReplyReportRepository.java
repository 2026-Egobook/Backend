package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlazaLetterReplyReportRepository extends JpaRepository<PlazaLetterReplyReport, Long> {

    boolean existsByReply_ReplyIdAndReporterId(Long replyId, Long reporterId);

    @Query("""
        select r.reply.replyId
        from PlazaLetterReplyReport r
        where r.reporterId = :reporterId
          and r.reply.replyId in :replyIds
    """)
    List<Long> findReportedReplyIds(Long reporterId, List<Long> replyIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PlazaLetterReplyReport r WHERE r.reporterId IN :reporterIds")
    void bulkDeleteByReporterIdIn(@Param("reporterIds") List<Long> reporterIds);

    /** 내가 신고당한 내역에서는 Replier ID를 Null로 익명화한다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PlazaLetterReplyReport r SET r.replierId = NULL WHERE r.replierId IN :replierIds")
    void bulkNullifyReplierId(@Param("replierIds") List<Long> replierIds);

    // 3회 이상 신고된 답장 삭제하고 신고 DB로 이동
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PlazaLetterReply r SET r.status = :status WHERE r.replyId = :replyId")
    void moveReplyToReportDbAndDelete(@Param("replyId") Long replyId, @Param("status") PlazaLetterReply.ReplyStatus status);

    // 신고된 답장에 대한 신고 횟수를 셈
    @Query("SELECT COUNT(r) FROM PlazaLetterReplyReport r WHERE r.reply.replyId = :replyId")
    long countByReply_ReplyId(@Param("replyId") Long replyId);

    @Query("""
        SELECT r
        FROM PlazaLetterReplyReport r
        JOIN FETCH r.reply
        ORDER BY r.createdAt DESC
    """)
    Slice<PlazaLetterReplyReport> findAllWithReply(Pageable pageable);

    //상세 조회
    @Query("""
        SELECT r
        FROM PlazaLetterReplyReport r
        JOIN FETCH r.reply
        WHERE r.reportId = :reportId
    """)
    Optional<PlazaLetterReplyReport> findByIdWithReply(@Param("reportId") Long reportId);
}
