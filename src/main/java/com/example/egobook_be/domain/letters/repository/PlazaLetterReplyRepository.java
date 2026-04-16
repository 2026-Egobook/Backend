package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlazaLetterReplyRepository extends JpaRepository<PlazaLetterReply, Long> {
    boolean existsByLetter(PlazaLetter letter);

    long countByReplierId(Long replierId);

    boolean existsByLetter_LetterId(Long letterId);

    @Query("SELECT r FROM PlazaLetterReply r JOIN FETCH r.letter WHERE r.replyId = :replyId")
    Optional<PlazaLetterReply> findByIdWithLetter(@Param("replyId") Long replyId);

    Slice<PlazaLetterReply> findByReplierIdOrderByReplyIdDesc(Long replierId, Pageable pageable);

    Optional<PlazaLetterReply> findByLetter(PlazaLetter letter);

    void deleteByThreadId(Long threadId);

    @Query("""
        select r
        from PlazaLetterReply r
        join fetch r.letter l
        where l.senderId = :userId
        order by r.createdAt desc
    """)
    Slice<PlazaLetterReply> findRepliesForMyLetters(
            @Param("userId") Long userId,
            Pageable pageable
    );

    boolean existsByReplierIdAndCreatedAtBetween(Long replierId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    // 내가 쓴 답장의 작성자 ID를 NULL로 익명화
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PlazaLetterReply r SET r.replierId = NULL, r.isAiGenerated = false WHERE r.replierId IN :replierIds")
    void bulkNullifyReplierId(@Param("replierIds") List<Long> replierIds);

    @Query("""
    select r
    from PlazaLetterReply r
    join fetch r.letter l
    where r.replierId = :replierId
    order by r.replyId desc
""")
    Slice<PlazaLetterReply> findMyRepliesWithLetter(
            @Param("replierId") Long replierId,
            Pageable pageable
    );

}
