package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final HomeMapper homeMapper;

    private final Integer ATTENDANCE_REWARD_INK = 3;

    /**
     * Home 화면에 필요한 데이터들을 반환해주는 함수
     * (1) 닉네임
     * (2) 레벨
     * (3) 잉크
     * (4) 사용자가 아직 읽지 않은 알림 개수
     * (5) 사용자가 열지 않은 오늘의 심리 지식 개수
     * (6) 오늘 최초 출석인지 여부
     * (7) 최초 출석 보상 잉크 값
     */
    @Transactional
    public HomeResDto getHomeData(Long userId){
        // 1. 사용자 정보 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 사용자가 아직 읽지 않은 알림 개수 확인
        Integer unReadNotificationCount = notificationRepository.countByUserAndIsReadIsFalse(user);

        // 3. 사용자가 열지 않은 오늘의 심리 지식 개수 -> 한다경님 merge하시면
        Integer unopenedPsychology = 1;

        // 4. 오늘 최초 출석인지 여부에 따라서 보상 잉크 값 결정
        Integer attendanceRewardInk = 0;
        if (user.isFirstAttendanceToday()) {
            attendanceRewardInk = ATTENDANCE_REWARD_INK;

        }
        else attendanceRewardInk = 0;
        HomeResDto resDto = homeMapper.toHomeResDto(user, unReadNotificationCount, unopenedPsychology, attendanceRewardInk);

        // 5. 사용자가 이미 출석 보상을 받았다고 설정
        user.rewardAttendance();

        return resDto;
    }
}
