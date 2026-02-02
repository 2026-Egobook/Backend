package com.example.egobook_be.domain.ego_room.scheduler;

import com.example.egobook_be.domain.ego_room.service.EgoStatsService; import com.example.egobook_be.domain.user.repository.UserRepository; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component @Slf4j @RequiredArgsConstructor public class EgoStatsScheduler {

    private final EgoStatsService egoStatsService;
    private final UserRepository userRepository;


    @Scheduled(cron = "0 0 3 * * *")
    public void updateDailyUserStats() {
        log.info("매일 새벽 3시마다 스케줄링");
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        userRepository.findAll().forEach(user -> {
            try {
                egoStatsService.calculateAndSaveStats(user.getId(), year, month);
            } catch (Exception e) {
                log.error("유저 {}번 통계 계산 중 오류 발생: {}", user.getId(), e.getMessage());
            }
        });

        log.info("주간 통계 자동 갱신 완료");
    }
}