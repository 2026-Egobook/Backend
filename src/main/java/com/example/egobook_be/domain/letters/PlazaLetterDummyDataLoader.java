package com.example.egobook_be.domain.letters;

import com.example.egobook_be.domain.letters.domain.PlazaLetter;
import com.example.egobook_be.domain.letters.domain.PlazaLetterMode;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Profile({"local", "dev"})
@Component
@RequiredArgsConstructor
public class PlazaLetterDummyDataLoader implements CommandLineRunner {

    private final PlazaLetterRepository repo;

    @Override
    public void run(String... args) {
        // receiverId=1에 ARRIVED 편지 1개가 없으면 생성
        boolean exists = repo.findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(1L, PlazaLetterStatus.ARRIVED).isPresent();
        if (exists) return;

        OffsetDateTime now = OffsetDateTime.now();

        repo.save(PlazaLetter.builder()
                .senderId(1L)
                .receiverId(1L)
                .status(PlazaLetterStatus.ARRIVED)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("요즘 너무 지치는데… 어떻게 버티지?")
                .arrivedAt(now)
                .createdAt(now)
                .threadId(1L)
                .replyDeadlineAt(now.plusHours(24))
                .backgroundColor("BLUE")
                .build());
    }
}

