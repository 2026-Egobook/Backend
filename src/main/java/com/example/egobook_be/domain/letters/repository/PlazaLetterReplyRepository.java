package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.domain.PlazaLetterReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaLetterReplyRepository extends JpaRepository<PlazaLetterReply, Long> {
    boolean existsByLetterId(Long letterId);

}
