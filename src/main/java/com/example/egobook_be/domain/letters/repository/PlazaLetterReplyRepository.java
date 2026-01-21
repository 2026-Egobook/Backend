package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaLetterReplyRepository extends JpaRepository<PlazaLetterReply, Long> {
    boolean existsByLetterId(Long letterId);


    Slice<PlazaLetterReply> findByReplierIdOrderByReplyIdDesc(Long replierId, Pageable pageable);

    void deleteByThreadId(Long threadId);

}
