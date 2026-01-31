package com.example.egobook_be.global.scheduler;

import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySchedular {
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    /**
     * 모든 사용자들의 일일 미션을 초기화하는 스케줄러 함수
     * - 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetAllDailyMissions(){
        log.info("🕛 [DailySchedular] 일일 미션 초기화 작업 시작");
        long startTime = System.currentTimeMillis();
        missionRepository.resetAllDailyMissions();
        long endTime = System.currentTimeMillis();
        log.info("🕛 [DailySchedular] 일일 미션 초기화 작업 종료. (소요시간: {}ms)", endTime-startTime);
    }

    /**
     * 모든 사용자들을 출석 보상을 받을 수 있는 상태로 변경해주는 스케줄러 함수
     * - 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetUserAttendanceStatus(){
        log.info("🕛 [DailySchedular] 사용자 일일 출석 보상 설정 작업 시작");
        long startTime = System.currentTimeMillis();
        userRepository.resetAllAttendancesStatus();
        long endTime = System.currentTimeMillis();
        log.info("🕛 [DailySchedular] 사용자 일일 출석 보상 설정 작업 종료. (소요시간: {}ms)", endTime-startTime);
    }
}
