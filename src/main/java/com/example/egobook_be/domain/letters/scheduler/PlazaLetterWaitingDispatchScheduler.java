package com.example.egobook_be.domain.letters.scheduler;

import com.example.egobook_be.domain.letters.service.scheduler.PlazaLetterWaitingDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlazaLetterWaitingDispatchScheduler {

    private final PlazaLetterWaitingDispatchService dispatchService;

    @Scheduled(fixedDelay = 5 * 60 * 1000L) // 5분마다
    public void run() {
        // 메서드 호출을 dispatchWaitingLetters로 수정
        dispatchService.dispatchWaitingLetters();
    }
}
