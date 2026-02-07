package com.example.egobook_be.domain.letters.scheduler;

import com.example.egobook_be.domain.letters.service.scheduler.PlazaLetterGiveUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlazaLetterGiveUpScheduler {

    private final PlazaLetterGiveUpService giveUpService;

    @Scheduled(fixedDelay = 5 * 60 * 1000L) // 5분마다
    public void run() {
        try {
            int count = giveUpService.autoGiveUpExpiredLetters();
            if (count > 0) {
                log.info("Auto give-up processed: {}", count);
            }
        } catch (Exception e) {
            log.error("Auto give-up scheduler failed", e);
        }
    }
}

