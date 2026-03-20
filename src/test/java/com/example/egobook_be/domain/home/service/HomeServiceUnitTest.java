package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceUnitTest {

    @InjectMocks
    private HomeService homeService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private InkLogRepository inkLogRepository;
    @Mock
    private HomeMapper homeMapper;

    @Test
    @DisplayName("[성공 1] 홈 화면에 접속해서 출석 보상 수령")
    void successGetDailyReward() {
        // ================ Given ================
        // 1. 객체 생성
        Long userId = 1L;
        int unreadNotificationCount = 2;
        boolean hasUnopenedPsychology = true;
        int attendanceRewardInk = 3;
        User mockUser = User.builder()
                .id(userId)
                .accountCode("testAccountCode")
                .email("test@example.com")
                .nickname("testNickname")
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .ink(10)
                .build();

        HomeResDto mockResDto = new HomeResDto(
                mockUser.getId(),
                mockUser.getNickname(),
                mockUser.getLevel(),
                mockUser.getInk(),
                unreadNotificationCount,
                hasUnopenedPsychology,
                mockUser.isFirstAttendanceToday(),
                attendanceRewardInk
        );

        // 2. Stub
        when(userRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(mockUser));
        when(notificationRepository.countByUserAndIsReadIsFalse(any(User.class))).thenReturn(2);
        when(inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(eq(mockUser), eq(InkLogType.FIRST_PSYCHOLOGY_VIEW), any(LocalDateTime.class)))
                .thenReturn(false); // 심리 지식을 열람하지 않은 상태 세팅 (exists가 false 반환)
        when(homeMapper.toHomeResDto(any(User.class), anyInt(), anyBoolean(), anyInt())).thenReturn(mockResDto); // Mapper 동작 세팅

        // ================ When ================
        HomeResDto result = homeService.getHomeData(userId);

        // ================ Then ================
        assertNotNull(result);

        /*
         * 검증 1: 잉크 보상이 0보다 크므로 InkLog가 1번 저장되었는지 확인
         * 검증 2: Mapper에 정확한 인자값들이 전달되었는지 확인
         */
        verify(inkLogRepository, times(1)).save(any(InkLog.class));
        verify(homeMapper, times(1)).toHomeResDto(mockUser, unreadNotificationCount, hasUnopenedPsychology, attendanceRewardInk);
    }

    @Test
    @DisplayName("[성공 2] 최초 출석이 아닌 경우 출석 보상 수령 안됨")
    void successDontGetDailyReward() {
        // ================ Given ================
        // 1. 객체 생성
        Long userId = 1L;
        int unreadNotificationCount = 0;
        boolean hasUnopenedPsychology = false;
        int attendanceRewardInk = 0;
        User mockUser = User.builder()
                .id(userId)
                .accountCode("testAccountCode")
                .email("test@example.com")
                .nickname("testNickname")
                .isFirstAttendanceToday(false)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .ink(10)
                .build();

        HomeResDto mockResDto = new HomeResDto(
                mockUser.getId(),
                mockUser.getNickname(),
                mockUser.getLevel(),
                mockUser.getInk(),
                unreadNotificationCount,
                hasUnopenedPsychology,
                mockUser.isFirstAttendanceToday(),
                attendanceRewardInk
        );

        // 2. Stub
        when(userRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(mockUser));
        when(notificationRepository.countByUserAndIsReadIsFalse(any(User.class))).thenReturn(0);
        when(inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(eq(mockUser), eq(InkLogType.FIRST_PSYCHOLOGY_VIEW), any(LocalDateTime.class)))
                .thenReturn(true); // 이미 심리 지식을 열람한 상태 세팅
        when(homeMapper.toHomeResDto(any(User.class), anyInt(), anyBoolean(), anyInt())).thenReturn(mockResDto);

        // ================ When ================
        HomeResDto result = homeService.getHomeData(userId);

        // ================ Then ================
        assertNotNull(result);
        /*
         * 검증 1: 보상이 0이므로 InkLog 저장이 단 한 번도 호출되지 않아야 함
         * 검증 2: Mapper에 정확한 인자값(보상 0)이 전달되었는지 확인
         */
        verify(inkLogRepository, never()).save(any(InkLog.class));
        verify(homeMapper, times(1)).toHomeResDto(mockUser, 0, false, 0);
    }

    @Test
    @DisplayName("[실패 1] 존재하지 않는 사용자")
    void failUserNotFound(){
        // ================ Given ================
        Long userId = 1L;
        when(userRepository.findByIdWithLock(userId)).thenReturn(Optional.empty());

        // ================ When & Then ================
        CustomException exception = assertThrows(CustomException.class, () -> {
            homeService.getHomeData(userId);
        });
        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}