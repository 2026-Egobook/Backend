package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.HomeAbilityResDto;
import com.example.egobook_be.domain.home.dto.HomeActivityResDto;
import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.dto.HomeSettingResDto;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class HomeService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AbilityRepository abilityRepository;
    private final MissionRepository missionRepository;
    private final InkLogRepository inkLogRepository;
    private final HomeMapper homeMapper;
    private final InkLogUtil inkLogUtil;

    private final Integer ATTENDANCE_REWARD_INK = 3;

    /**
     * Home 화면에 필요한 데이터들을 반환해주는 함수
     * (1) 닉네임
     * (2) 레벨
     * (3) 잉크
     * (4) 사용자가 아직 읽지 않은 알림 개수
     * (5) 사용자의 오늘의 심리 지식 열람 여부 
     * (6) 오늘 최초 출석인지 여부
     * (7) 최초 출석 보상 잉크 값
     */
    @Transactional
    public HomeResDto getHomeData(Long userId){
        /*
         * 1. 사용자 정보 가져오기
         * - 해당 API가 동시에 호출되면 동시성 이슈로 2개의 스레드가 처리됨으로써 잉크 보상이 2번 주어질 수 있다.
         * - 따라서 해당 사용자의 데이터에 비관적 락(Pessimistic Lock)을 적용하였습니다.
         */
        User user = userRepository.findByIdWithLock(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 사용자가 아직 읽지 않은 알림 개수 확인
        Integer unReadNotificationCount = notificationRepository.countByUserAndIsReadIsFalse(user);

        // 3. 사용자가 열지 않은 오늘의 심리 지식 여부
        Boolean hasUnopenedPsychology = hasUnopenedPsychologyKnowledge(user);

        // 4. 오늘 최초 출석인지 여부에 따라서 보상 잉크 값 결정
        int attendanceRewardInk = user.checkFirstAttendanceTodayAndGetReward(ATTENDANCE_REWARD_INK);;
        if (attendanceRewardInk > 0) {
            inkLogRepository.save(InkLog.builder()
                            .user(user)
                            .amount(attendanceRewardInk)
                            .reason(InkLogType.ATTENDANCE_REWARD)
                            .build()
                    );
        }
        else attendanceRewardInk = 0;
        HomeResDto resDto = homeMapper.toHomeResDto(user, unReadNotificationCount, hasUnopenedPsychology, attendanceRewardInk);

        return resDto;
    }

    /**
     * Home 화면의 활동 목록 정보를 조회하는 함수
     * (1) 오늘의 하루 미션 최종 성공 여부
     * (2) 감정일기 작성 수행 여부
     * (3) 편지 쓰기 수행 여부
     * (4) 오늘의 질문 답변 수행 여부
     * (5) 연속 진행 주차 값
     * (6) 이번주 (월 ~ 일) 미션 수행 여부 리스트
     */
    @Transactional
    public HomeActivityResDto getHomeActivities(Long userId){
        // 1. 사용자 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 해당 사용자와 1대1로 연결된 Mission 데이터 가져오기
        Mission userMission = missionRepository.findByUser(user).orElse(null);

        // 3. 아직 미션 데이터가 없는 경우 (회원가입 직후 등), 기본값(모두 false/0) 반환
        if (userMission == null) {
            return homeMapper.toEmptyHomeActivityResDto();
        }

        // 4. 사용자의 주간 미션 현황을 최신화한다.
        userMission.checkAndResetWeekly(LocalDate.now());

        /*
         * 5. Entity -> DTO 변환
         * Mission 엔티티에 만들어둔 편의 메서드를 활용하여 요일별 상태를 List로 변환합니다.
         */
        return homeMapper.toHomeActivityResDto(userMission);
    }

    /**
     * 해당 사용자의 능력치를 가져오는 함수
     */
    @Transactional(readOnly = true)
    public HomeAbilityResDto getHomeAbilities(Long userId){
        // 1. 사용자 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 해당 사용자의 Ability 가져오기
        Ability userAbility = abilityRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.ABILITY_NOT_FOUND));

        // 3. 해당 사용자의 Ability 반환
        return homeMapper.toHomeAbilityResDto(userAbility);
    }

    @Transactional(readOnly = true)
    public HomeSettingResDto getHomeSettings(Long userId){
        // 1. 사용자 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        // 2. 사용자의 AccountCode 반환
        return homeMapper.toHomeSettingResDto(user);
    }

    /**
     * 사용자가 아직 읽지 않은 오늘의 심리 지식이 있는지 여부
     * - 잉크 획득 로그에서 아직 오늘의 심리 지식으로 얻은 잉크가 없는 경우: true 반환
     */
    private boolean hasUnopenedPsychologyKnowledge(User user){
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return !inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(
                user, InkLogType.FIRST_PSYCHOLOGY_VIEW, startOfToday);
    }


}
