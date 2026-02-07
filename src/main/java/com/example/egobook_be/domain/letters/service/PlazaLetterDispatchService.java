package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PlazaLetterDispatchService {

    private final PlazaLetterRepository plazaLetterRepository;
    private final UserRepository userRepository;

    private static final int BATCH_SIZE = 20;

    /**
     * WAITING 편지를 수신 가능한 유저에게 분배
     */
    @Transactional
    public void dispatchWaitingLetters() {
        OffsetDateTime now = OffsetDateTime.now();

        List<PlazaLetter> waitingLetters =
                plazaLetterRepository.findWaitingLetters(PageRequest.of(0, BATCH_SIZE));

        for (PlazaLetter letter : waitingLetters) {

            Long senderId = letter.getSenderId();

            // sender 제외 + 쿨다운 해제된 유저 후보
            List<Long> candidates =
                    userRepository.findHighReplyRateCandidates(senderId, 30);

            if (candidates.isEmpty()) {
                // 👉 받을 사람 없으면 그대로 WAITING 유지
                continue;
            }

            Long receiverId =
                    candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

            // ===== 편지 도착 처리 =====
            letter.assignReceiver(
                    receiverId,
                    now,
                    now.plusHours(24)
            );
        }
    }
}
