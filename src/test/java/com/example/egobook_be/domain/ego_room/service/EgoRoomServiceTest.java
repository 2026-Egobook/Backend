package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.dto.WeeklyCounselResDto;
import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EgoRoomServiceTest {

    @Mock
    private DailyPraiseRepository dailyPraiseRepository;
    @Mock private WeeklyCounselRepository weeklyCounselRepository;
    @Mock private UserRepository userRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private DailyPraiseAiService dailyPraiseAiService;
    @Mock private WeeklyAnalysisAiService weeklyAnalysisAiService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EgoRoomService egoRoomService;

    @Test
    @DisplayName("createDailyPraise_유저없음_실패")
    void createDailyPraise_userNotFound_fail() {
        // given
        Long userId = 999L;
        LocalDate date = LocalDate.now();
        when(dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // 유저 없음

        // when & then
        assertThrows(RuntimeException.class, () -> {
            egoRoomService.createDailyPraise(userId, date);
        });
    }

    @Test
    @DisplayName("일기 데이터가 있을 때 일간 칭찬서를 생성 후 알림")
    void createDailyPraise_diaryExists_success() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        User user = User.builder().id(userId).nickname("테스트유저").build();
        Diary diary = Diary.builder().content("오늘의 일기").build();

        when(dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(diaryRepository.findByUserIdAndDate(userId, date)).thenReturn(List.of(diary));
        when(dailyPraiseAiService.getPraise(anyString())).thenReturn("AI 칭찬 내용");

        // when
        egoRoomService.createDailyPraise(userId, date);

        // then
        verify(dailyPraiseRepository, times(1)).save(any(DailyPraise.class));
        verify(notificationService, times(1)).createNotification(eq(userId), eq(NotificationType.PRAISE), any(), any());
    }

    @Test
    @DisplayName("이미 해당 날짜에 칭찬서가 존재하면 생성 스킵")
    void createDailyPraise_alreadyExists_skip() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        when(dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date))
                .thenReturn(Optional.of(mock(DailyPraise.class)));

        // when
        egoRoomService.createDailyPraise(userId, date);

        // then
        verify(dailyPraiseAiService, never()).getPraise(anyString());
        verify(dailyPraiseRepository, never()).save(any());
    }

    @Test
    @DisplayName("일기가 없을 때는 칭찬서 생성 스킵")
    void createDailyPraise_noDiary_skip() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        when(dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));

        when(diaryRepository.findByUserIdAndDate(userId, date)).thenReturn(Collections.emptyList());

        // when
        egoRoomService.createDailyPraise(userId, date);

        // then
        verify(dailyPraiseRepository, never()).save(any(DailyPraise.class));
    }

    @Test
    @DisplayName("createWeeklyAnalysis_유저없음_실패")
    void createWeeklyAnalysis_userNotFound_fail() {
        // given
        Long userId = 999L;
        LocalDate startDate = LocalDate.of(2026, 3, 9);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(RuntimeException.class, () -> {
            egoRoomService.createWeeklyAnalysis(userId, startDate);
        });
    }

    @Test
    @DisplayName("주간 분석 리포트 생성 및 저장")
    void createWeeklyAnalysis_weeklyDataExists_success() {

        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 9);
        User user = User.builder().id(userId).nickname("테스트유저").counselingTone(CounselTone.SOFT).build();
        Diary diary = Diary.builder()
                .content("주간 일기")
                .build();

        // 가짜 생성일 주입
        ReflectionTestUtils.setField(diary, "createdAt", startDate.atStartOfDay());
        WeeklyCounselResDto aiRes = new WeeklyCounselResDto("테스트유저","요약", "칭찬", "개선", "조언", "응원");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(weeklyCounselRepository.existsByUserIdAndStartDate(userId, startDate)).thenReturn(false);
        when(diaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(any(), any(), any())).thenReturn(List.of(diary));
        when(weeklyAnalysisAiService.getAnalysis(any(), any(), any(), any())).thenReturn(aiRes);

        // when
        egoRoomService.createWeeklyAnalysis(userId, startDate);

        // then
        verify(weeklyCounselRepository, times(1)).save(any(WeeklyCounsel.class));
        verify(notificationService, times(1)).createNotification(eq(userId), eq(NotificationType.REPORT), any());
    }

    @Test
    @DisplayName("일기가 없으면 주간 분석서 생성 스킵")
    void createWeeklyAnalysis_noDiary_skip() {
        // given
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 9);
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(weeklyCounselRepository.existsByUserIdAndStartDate(userId, startDate)).thenReturn(false);

        when(diaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        egoRoomService.createWeeklyAnalysis(userId, startDate);

        // then
        verify(weeklyAnalysisAiService, never()).getAnalysis(any(), any(), any(), any());
        verify(weeklyCounselRepository, never()).save(any());
    }

}