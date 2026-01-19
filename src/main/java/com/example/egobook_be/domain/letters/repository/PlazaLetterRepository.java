package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.domain.PlazaLetter;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
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


}

