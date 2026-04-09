package com.example.egobook_be.domain.ego_room.scheduler;

import com.example.egobook_be.domain.ego_room.entity.DailyPraiseSendFailLog;
import com.example.egobook_be.domain.ego_room.entity.WeeklyReportSendFailLog;
import com.example.egobook_be.domain.ego_room.enums.SendFailReason;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyReportSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiScheduler {

    private final UserRepository userRepository;
    private final EgoRoomService egoRoomService;
    private final DailyPraiseSendFailLogRepository dailyPraiseFailLogRepo;    // ← 추가
    private final WeeklyReportSendFailLogRepository weeklyReportFailLogRepo;  // ← 추가


    //0시 1분마다 실행
    @Scheduled(cron = "0 1 0 * * *") // 초 분 시 일 월 요일
    public void scheduleDailyPraise() {
        log.info("[스케줄러 시작] 일간 칭찬 생성 작업을 시작합니다.");

        // 기준 날짜 설정 (어제 날짜의 일기를 처리)
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 일간칭찬 true인 유저를 돌면서 처리
        List<User> targetUsers = userRepository.findByDailyPraiseTrue();

        for (User user : targetUsers) {
            try {
                egoRoomService.createDailyPraise(user.getId(), yesterday);
            } catch (Exception e) {
                log.error("[스케줄러 오류] 유저 {}의 일간 칭찬 생성 중 실패: {}", user.getId(), e.getMessage());

                // 실패 로그 저장
                dailyPraiseFailLogRepo.save(DailyPraiseSendFailLog.builder()
                        .userId(user.getId())
                        .targetDate(yesterday)
                        .reason(resolveReason(e))
                        .failedAt(LocalDateTime.now())
                        .build());
            }
        }

        log.info("[스케줄러 종료] 모든 유저의 일간 칭찬 생성이 완료되었습니다.");
    }

    //월요일 새벽 한시마다 수행
    @Scheduled(cron = "0 0 1 * * MON")
    public void scheduleWeeklyAnalysis() {
        log.info("[스케줄러 시작] 주간 분석 생성 작업을 시작합니다.");

        // 지난주 월요일 날짜 계산
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        List<User> targetUsers = userRepository.findAllByWeeklyAnalysisEnabledTrue();

        for (User user : targetUsers) {
            try {
                egoRoomService.createWeeklyAnalysis(user.getId(), lastMonday);
            } catch (Exception e) {
                log.error("[스케줄러 오류] 유저 {}의 주간 분석 생성 중 실패: {}", user.getId(), e.getMessage());

                // 실패 로그 저장
                weeklyReportFailLogRepo.save(WeeklyReportSendFailLog.builder()
                        .userId(user.getId())
                        .weekStartDate(lastMonday)
                        .reason(resolveReason(e))
                        .failedAt(LocalDateTime.now())
                        .build());
            }
        }

        log.info("[스케줄러 종료] 주간 분석 생성이 완료되었습니다.");
    }

    private SendFailReason resolveReason(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
        if (msg.contains("FCM") || msg.contains("TOKEN")) return SendFailReason.FCM_TOKEN_NOT_FOUND;
        if (msg.contains("TIMEOUT") || msg.contains("AI"))  return SendFailReason.AI_RESPONSE_TIMEOUT;
        if (msg.contains("USER"))                            return SendFailReason.USER_NOT_FOUND;
        return SendFailReason.UNKNOWN_ERROR;
    }
}