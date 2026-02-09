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
    private static final int RECEIVER_POOL_SIZE = 300;

    /**
     * WAITING 편지를 수신 가능한 유저에게 분배
     */
    @Transactional
    public void dispatchWaitingLetters() {
        OffsetDateTime now = OffsetDateTime.now();

        List<PlazaLetter> waitingLetters =
                plazaLetterRepository.findWaitingLetters(PageRequest.of(0, BATCH_SIZE));

        if (waitingLetters.isEmpty()) return;

        // 수신 가능 유저 풀: 1번만 조회
        List<Long> receiverPool = userRepository.findAvailableReceivers(
                now,
                PageRequest.of(0, RECEIVER_POOL_SIZE)
        );

        if (receiverPool.isEmpty()) return;

        for (PlazaLetter letter : waitingLetters) {
            Long senderId = letter.getSenderId();

            Long receiverId = pickRandomExcluding(receiverPool, senderId);
            if (receiverId == null) continue;

            letter.assignReceiver(receiverId, now, now.plusHours(24));
        }
    }

    private Long pickRandomExcluding(List<Long> pool, Long excluded) {
        int size = pool.size();
        if (size == 0) return null;

        // pool에 excluded만 있는 경우
        if (size == 1 && excluded != null && excluded.equals(pool.get(0))) {
            return null;
        }

        // 빠른 랜덤 재시도
        for (int i = 0; i < 5; i++) {
            Long picked = pool.get(ThreadLocalRandom.current().nextInt(size));
            if (excluded == null || !excluded.equals(picked)) {
                return picked;
            }
        }

        // fallback: 선형 탐색
        for (Long id : pool) {
            if (excluded == null || !excluded.equals(id)) {
                return id;
            }
        }
        return null;
    }
}
