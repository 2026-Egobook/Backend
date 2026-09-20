package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.entity.WeeklyReportSendFailLog;
import com.example.egobook_be.domain.ego_room.enums.SendFailReason;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyReportSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.letters.repository.AiRequestCountLogRepository;
import com.example.egobook_be.domain.letters.repository.BadWordBlockLogRepository;
import com.example.egobook_be.domain.letters.repository.LetterSendFailLogRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.dto.AdminContentResDto.ResendRes;
import com.example.egobook_be.domain.user.dto.AdminContentResDto.ResendResult;
import com.example.egobook_be.domain.user.dto.ResendReqDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminContentServiceUnitTest {

    @InjectMocks
    private AdminContentService adminContentService;

    @Mock private DailyPraiseSendFailLogRepository dailyPraiseFailLogRepo;
    @Mock private WeeklyReportSendFailLogRepository weeklyReportFailLogRepo;
    @Mock private LetterSendFailLogRepository letterFailLogRepo;
    @Mock private BadWordBlockLogRepository badWordBlockLogRepo;
    @Mock private DailyPraiseRepository dailyPraiseRepo;
    @Mock private WeeklyCounselRepository weeklyCounselRepo;
    @Mock private PlazaLetterRepository plazaLetterRepo;
    @Mock private EgoRoomService egoRoomService;
    @Mock private AiRequestCountLogRepository aiRequestCountLogRepo;
    @Mock private DiaryRepository diaryRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        });
    }

    @Nested
    @DisplayName("주간 리포트 수동 재발송")
    class ResendWeeklyReportTest {

        @Test
        @DisplayName("주간 리포트 재발송 성공")
        void 주간_리포트_재발송_성공() {
            // given
            Long failId = 1L;
            WeeklyReportSendFailLog failLog = createFailLog(failId, false);
            given(weeklyReportFailLogRepo.findById(failId)).willReturn(Optional.of(failLog));
            given(weeklyCounselRepo.existsByUserIdAndStartDate(10L, LocalDate.of(2026, 7, 6)))
                    .willReturn(false);
            given(egoRoomService.resendWeeklyAnalysis(10L, LocalDate.of(2026, 7, 6)))
                    .willReturn(true);

            // when
            ResendRes result = adminContentService.resendWeeklyReport(createRequest(failId));

            // then
            ResendResult resendResult = result.getResults().get(0);
            assertThat(result.getSuccessCount()).isEqualTo(1);
            assertThat(result.getFailCount()).isZero();
            assertThat(resendResult.getStatus()).isEqualTo("SUCCESS");
            assertThat(resendResult.isResent()).isTrue();
            assertThat(failLog.isResent()).isTrue();
        }

        @Test
        @DisplayName("주간 리포트 재발송 실패 - 이미 리포트가 존재함")
        void 주간_리포트_재발송_실패_이미_리포트가_존재함() {
            // given
            Long failId = 2L;
            WeeklyReportSendFailLog failLog = createFailLog(failId, false);
            given(weeklyReportFailLogRepo.findById(failId)).willReturn(Optional.of(failLog));
            given(weeklyCounselRepo.existsByUserIdAndStartDate(10L, LocalDate.of(2026, 7, 6)))
                    .willReturn(true);

            // when
            ResendRes result = adminContentService.resendWeeklyReport(createRequest(failId));

            // then
            ResendResult resendResult = result.getResults().get(0);
            assertThat(result.getSuccessCount()).isZero();
            assertThat(result.getFailCount()).isEqualTo(1);
            assertThat(resendResult.getStatus()).isEqualTo("FAIL");
            assertThat(resendResult.getReason()).isEqualTo("ALREADY_EXISTS");
            assertThat(resendResult.isResent()).isFalse();
            assertThat(failLog.isResent()).isFalse();
            verify(egoRoomService, never()).resendWeeklyAnalysis(any(), any());
        }

        @Test
        @DisplayName("주간 리포트 재발송 실패 - 작성된 일기가 없음")
        void 주간_리포트_재발송_실패_작성된_일기가_없음() {
            // given
            Long failId = 3L;
            WeeklyReportSendFailLog failLog = createFailLog(failId, false);
            given(weeklyReportFailLogRepo.findById(failId)).willReturn(Optional.of(failLog));
            given(weeklyCounselRepo.existsByUserIdAndStartDate(10L, LocalDate.of(2026, 7, 6)))
                    .willReturn(false);
            given(egoRoomService.resendWeeklyAnalysis(10L, LocalDate.of(2026, 7, 6)))
                    .willReturn(false);

            // when
            ResendRes result = adminContentService.resendWeeklyReport(createRequest(failId));

            // then
            ResendResult resendResult = result.getResults().get(0);
            assertThat(result.getSuccessCount()).isZero();
            assertThat(result.getFailCount()).isEqualTo(1);
            assertThat(resendResult.getStatus()).isEqualTo("FAIL");
            assertThat(resendResult.getReason()).isEqualTo("NO_DIARY");
            assertThat(resendResult.isResent()).isFalse();
            assertThat(failLog.isResent()).isFalse();
        }
    }

    private ResendReqDto createRequest(Long failId) {
        ResendReqDto reqDto = new ResendReqDto();
        ReflectionTestUtils.setField(reqDto, "failIds", List.of(failId));
        return reqDto;
    }

    private WeeklyReportSendFailLog createFailLog(Long failId, boolean resent) {
        return WeeklyReportSendFailLog.builder()
                .id(failId)
                .userId(10L)
                .weekStartDate(LocalDate.of(2026, 7, 6))
                .reason(SendFailReason.UNKNOWN_ERROR)
                .failedAt(LocalDateTime.of(2026, 7, 13, 0, 0))
                .resent(resent)
                .build();
    }
}
