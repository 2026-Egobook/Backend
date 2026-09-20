package com.example.egobook_be.domain.ego_room.scheduler;

import com.example.egobook_be.domain.ego_room.repository.DailyPraiseSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyReportSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiSchedulerUnitTest {

    @InjectMocks
    private AiScheduler aiScheduler;

    @Mock private UserRepository userRepository;
    @Mock private EgoRoomService egoRoomService;
    @Mock private DailyPraiseSendFailLogRepository dailyPraiseFailLogRepo;
    @Mock private WeeklyReportSendFailLogRepository weeklyReportFailLogRepo;

    @Test
    @DisplayName("일간 칭찬 생성 대상에서 탈퇴 대기 사용자 제외")
    void 일간_칭찬_생성_대상에서_탈퇴_대기_사용자_제외_성공() {
        // given
        given(userRepository.findByDailyPraiseTrueAndStatusNot(UserStatus.WITHDRAW_PENDING))
                .willReturn(List.of());

        // when
        aiScheduler.scheduleDailyPraise();

        // then
        verify(userRepository).findByDailyPraiseTrueAndStatusNot(UserStatus.WITHDRAW_PENDING);
        verifyNoInteractions(egoRoomService, dailyPraiseFailLogRepo);
    }

    @Test
    @DisplayName("주간 리포트 생성 대상에서 탈퇴 대기 사용자 제외")
    void 주간_리포트_생성_대상에서_탈퇴_대기_사용자_제외_성공() {
        // given
        given(userRepository.findAllByWeeklyAnalysisEnabledTrueAndStatusNot(UserStatus.WITHDRAW_PENDING))
                .willReturn(List.of());

        // when
        aiScheduler.scheduleWeeklyAnalysis();

        // then
        verify(userRepository).findAllByWeeklyAnalysisEnabledTrueAndStatusNot(UserStatus.WITHDRAW_PENDING);
        verifyNoInteractions(egoRoomService, weeklyReportFailLogRepo);
    }
}
