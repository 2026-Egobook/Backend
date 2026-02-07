package com.example.egobook_be.domain.letters.service.scheduler;

import com.example.egobook_be.domain.letters.service.ai.PlazaLetterAiReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlazaLetterBatchScheduler {

    private final com.example.egobook_be.domain.letters.service.scheduler.PlazaLetterGiveUpService giveUpService;
    private final PlazaLetterWaitingDispatchService waitingDispatchService;
    private final PlazaLetterAiReplyService aiReplyService;

    // 1) 24시간 초과 자동 포기 처리: 1분마다
    @Scheduled(fixedDelay = 60_000)
    public void autoGiveUpExpiredLetters() {
        int processed = giveUpService.autoGiveUpExpiredLetters();
        if (processed > 0) {
            log.info("[PlazaLetter] autoGiveUp processed={}", processed);
        }
    }

    // 2) 수신 가능(포기 후 4시간 경과) 유저에게 WAITING 편지 배정: 1분마다
    @Scheduled(fixedDelay = 60_000)
    public void dispatchWaitingLetters() {
        int dispatched = waitingDispatchService.dispatchWaitingLetters();
        if (dispatched > 0) {
            log.info("[PlazaLetter] waiting dispatch dispatched={}", dispatched);
        }
    }

    // 3) 48시간 무응답 AI 답장 대체: 5분마다
    @Scheduled(fixedDelay = 300_000)
    public void generateAiReplies() {
        int replaced = aiReplyService.generateAiReplies(200);  // 원하는 배치 사이즈를 넣어주기
        if (replaced > 0) {
            log.info("[PlazaLetter] aiReply generated={}", replaced);
        }
    }
}
