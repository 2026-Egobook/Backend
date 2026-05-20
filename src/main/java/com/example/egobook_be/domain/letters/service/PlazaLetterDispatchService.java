package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.entity.LetterSendFailLog;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.LetterSendFailLogRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlazaLetterDispatchService {

    private final PlazaLetterRepository plazaLetterRepository;
    private final UserRepository userRepository;
    private final RestrictionGuardService restrictionGuardService;
    private final LetterSendFailLogRepository letterFailLogRepo;

    private static final int BATCH_SIZE = 20;
    private static final int RECEIVER_POOL_SIZE = 300;

    /**
     * WAITING 편지를 수신 가능한 유저에게 분배
     */
    @Transactional
    public void dispatchWaitingLetters() {
        log.info("[PlazaLetterDispatchService] dispatchWaitingLetters Start");
        LocalDateTime now = LocalDateTime.now();

        List<PlazaLetter> waitingLetters =
                plazaLetterRepository.findWaitingLetters(PageRequest.of(0, BATCH_SIZE));

        if (waitingLetters.isEmpty()) return;

        // 수신 가능 유저 풀: 1번만 조회
        List<Long> receiverPool = userRepository.findAvailableReceivers(
                now,
                PageRequest.of(0, RECEIVER_POOL_SIZE)
        );

        if (receiverPool.isEmpty()) {
            for (PlazaLetter letter : waitingLetters) {
                letterFailLogRepo.save(LetterSendFailLog.builder()
                        .letterId(letter.getLetterId())
                        .failedAt(LocalDateTime.now())
                        .reason("수신 가능한 유저 없음")
                        .build());
            }
            return;
        }

        // LETTER 제재 사용자 수신자 풀에서 제외
        Set<Long> restrictedIds = restrictionGuardService.getActivelyRestrictedUserIds(RestrictionDomainType.LETTER);
        if (!restrictedIds.isEmpty()) {
            receiverPool = receiverPool.stream()
                    .filter(id -> !restrictedIds.contains(id))
                    .collect(Collectors.toList());
            if (receiverPool.isEmpty()) {
                for (PlazaLetter letter : waitingLetters) {
                    letterFailLogRepo.save(LetterSendFailLog.builder()
                            .letterId(letter.getLetterId())
                            .failedAt(LocalDateTime.now())
                            .reason("수신 가능한 유저 없음")
                            .build());
                }
                return;
            }
        }

        for (PlazaLetter letter : waitingLetters) {
            Long senderId = letter.getSenderId();
            Long receiverId = pickRandomExcluding(receiverPool, senderId);

            if (receiverId == null) {
                // 기존엔 그냥 continue → 실패 로그 저장 추가
                letterFailLogRepo.save(LetterSendFailLog.builder()
                        .letterId(letter.getLetterId())
                        .failedAt(LocalDateTime.now())
                        .reason("수신 가능한 유저 없음")
                        .build());
                continue;
            }
            letter.assignReceiver(receiverId, now, now.plusHours(24));
        }
        log.info("[PlazaLetterDispatchService] dispatchWaitingLetters End");
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