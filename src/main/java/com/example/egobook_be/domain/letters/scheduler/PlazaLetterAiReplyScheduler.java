package com.example.egobook_be.domain.letters.scheduler;

import com.example.egobook_be.domain.letters.service.ai.PlazaLetterAiReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlazaLetterAiReplyScheduler {

    private final PlazaLetterAiReplyService aiReplyService;

    @Scheduled(fixedDelay = 10 * 60 * 1000L) // 10분마다
    public void run() {
        try {
            int count = aiReplyService.generateAiReplies(50);
            if (count > 0) {
                log.info("AI replies generated: {}", count);
            }
        } catch (Exception e) {
            log.error("AI reply scheduler failed", e);
        }
    }
}

