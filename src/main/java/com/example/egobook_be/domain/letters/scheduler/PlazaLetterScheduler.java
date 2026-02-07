package com.example.egobook_be.domain.letters.scheduler;

import com.example.egobook_be.domain.letters.service.PlazaLetterDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlazaLetterScheduler {

    private final PlazaLetterDispatchService dispatchService;

    // 5분마다 실행 (조절 가능)
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void dispatchWaitingLetters() {
        dispatchService.dispatchWaitingLetters();
    }
}
