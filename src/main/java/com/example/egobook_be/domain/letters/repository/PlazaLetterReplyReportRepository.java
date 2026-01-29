package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
