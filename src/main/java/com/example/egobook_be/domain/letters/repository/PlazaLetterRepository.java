package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PlazaLetterRepository extends JpaRepository<PlazaLetter, Long> {

    Optional<PlazaLetter> findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(Long receiverId, PlazaLetterStatus status);

    boolean existsBySenderIdAndCreatedAtBetween(Long senderId, OffsetDateTime start, OffsetDateTime end);

    Optional<PlazaLetter> findByThreadId(Long threadId);

    void deleteByThreadId(Long threadId);

    long countByReceiverIdAndArrivedAtBetween(Long receiverId, OffsetDateTime start, OffsetDateTime end);

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
            @Param("threshold") OffsetDateTime threshold,
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

}

