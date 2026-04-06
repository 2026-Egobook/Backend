package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlazaLetterRepository extends JpaRepository<PlazaLetter, Long> {

    Optional<PlazaLetter> findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(Long receiverId, PlazaLetterStatus status);

    Integer countBySenderId(Long senderId);

    boolean existsBySenderIdAndCreatedAtBetween(Long senderId, LocalDateTime start, LocalDateTime end);

    Optional<PlazaLetter> findByThreadId(Long threadId);

    void deleteByThreadId(Long threadId);

    long countByReceiverIdAndArrivedAtBetween(Long receiverId, LocalDateTime start, LocalDateTime end);

    List<PlazaLetter> findByLetterIdIn(List<Long> letterIds);

    Slice<PlazaLetter> findByReceiverIdOrderByArrivedAtDesc(
            Long receiverId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PlazaLetter l
           set l.status = :newStatus,
               l.fromLabel = :newFromLabel
         where l.senderId = :senderId
           and l.status = com.example.egobook_be.domain.letters.entity.PlazaLetterStatus.SENT
           and l.createdAt <= :threshold
    """)
    int bulkMarkAiReplied(
            @Param("senderId") Long senderId,
            @Param("threshold") LocalDateTime threshold,
            @Param("newStatus") PlazaLetterStatus newStatus,
            @Param("newFromLabel") String newFromLabel
    );

    @Query("""
        select l
          from PlazaLetter l
         where l.senderId = :senderId
         order by l.createdAt desc, l.letterId desc
    """)
    Slice<PlazaLetter> findMySentLettersSlice(
            @Param("senderId") Long senderId,
            Pageable pageable
    );

    /** 탈퇴한 Sender의 ID를 NULL로 변경 (익명화) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PlazaLetter l SET l.senderId = NULL WHERE l.senderId IN :senderIds")
    void bulkNullifySenderId(@Param("senderIds") List<Long> senderIds);

    /** 탈퇴한 Receiver의 ID를 NULL로 변경 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PlazaLetter l SET l.receiverId = NULL WHERE l.receiverId IN :receiverIds")
    void bulkNullifyReceiverId(@Param("receiverIds") List<Long> receiverIds);

    /** Sender와 Receiver가 모두 사라진(NULL) "완전 고아 편지" 삭제(이 메서드는 두 ID가 모두 NULL인 데이터만 지웁니다) */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PlazaLetter l WHERE l.senderId IS NULL AND l.receiverId IS NULL")
    void bulkDeleteOrphanedLetters();


    @Query("""
        select l
        from PlazaLetter l
        where l.createdAt <= :cutoff
          and l.status not in (:replied, :aiReplied)
          and not exists (
              select 1
              from PlazaLetterReply r
              where r.letter = l
          )
        order by l.createdAt asc
    """)
    List<PlazaLetter> findAiReplyTargets(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("replied") PlazaLetterStatus replied,
            @Param("aiReplied") PlazaLetterStatus aiReplied,
            Pageable pageable
    );

    @Query("SELECT l FROM PlazaLetter l " +
            "WHERE l.replyDeadlineAt <= :now " +
            "AND l.status IN (:arrived, :deferred) " +
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM PlazaLetterReply r " +
            "   WHERE r.letter.letterId = l.letterId" +
            ") " +
            "ORDER BY l.replyDeadlineAt ASC")
    List<PlazaLetter> findGiveUpTargets(
            @Param("now") LocalDateTime now,
            @Param("arrived") PlazaLetterStatus arrived,
            @Param("deferred") PlazaLetterStatus deferred,
            Pageable pageable
    );



    @Query("""
    select l
    from PlazaLetter l
    where l.status = :waiting
      and l.receiverId is null
      and l.senderId <> :receiverId
    order by l.createdAt asc
""")
    List<PlazaLetter> findWaitingLettersForReceiver(
            @Param("receiverId") Long receiverId,
            @Param("waiting") PlazaLetterStatus waiting,
            Pageable pageable
    );


    @Query("""
        select l
        from PlazaLetter l
        where l.status = com.example.egobook_be.domain.letters.entity.PlazaLetterStatus.WAITING
        order by l.createdAt asc
    """)
    List<PlazaLetter> findWaitingLetters(Pageable pageable);

    @Query("""
        select l
        from PlazaLetter l
        where l.receiverId = :userId
          and l.status = com.example.egobook_be.domain.letters.entity.PlazaLetterStatus.DEFERRED
        order by l.arrivedAt desc, l.letterId desc
    """)
    Slice<PlazaLetter> findMyDeferredInboxSlice(
            @Param("userId") Long userId,
            Pageable pageable
    );


    @Query("""
    select l
    from PlazaLetter l
    where l.letterId = :letterId
      and l.receiverId = :userId
      and l.status in (
        com.example.egobook_be.domain.letters.entity.PlazaLetterStatus.ARRIVED,
        com.example.egobook_be.domain.letters.entity.PlazaLetterStatus.DEFERRED
      )
""")
    Optional<PlazaLetter> findInboxLetterForReply(
            @Param("letterId") Long letterId,
            @Param("userId") Long userId
    );

}

