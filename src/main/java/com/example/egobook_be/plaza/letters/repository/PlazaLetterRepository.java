package com.example.egobook_be.plaza.letters.repository;

import com.example.egobook_be.plaza.letters.domain.PlazaLetter;
import com.example.egobook_be.plaza.letters.domain.PlazaLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlazaLetterRepository extends JpaRepository<PlazaLetter, Long> {

    Optional<PlazaLetter> findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(
            Long receiverId,
            PlazaLetterStatus status
    );
}

