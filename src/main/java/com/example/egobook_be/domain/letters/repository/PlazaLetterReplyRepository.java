package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlazaLetterReplyRepository extends JpaRepository<PlazaLetterReply, Long> {
    boolean existsByLetterId(Long letterId);


    Slice<PlazaLetterReply> findByReplierIdOrderByReplyIdDesc(Long replierId, Pageable pageable);

    Optional<PlazaLetterReply> findByLetterId(Long letterId);

    void deleteByThreadId(Long threadId);

    @Query("""
        select r
        from PlazaLetterReply r
        join PlazaLetter l on r.letterId = l.letterId
        where l.senderId = :userId
        order by r.createdAt desc
    """)
    Slice<PlazaLetterReply> findRepliesForMyLetters(
            @Param("userId") Long userId,
            Pageable pageable
    );

}
