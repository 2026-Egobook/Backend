package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportDetailResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.report.enums.ReportErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LetterReportAdminServiceUnitTest {

    @InjectMocks
    private LetterReportAdminService letterReportAdminService;

    @Mock private PlazaLetterReportRepository letterReportRepository;
    @Mock private PlazaLetterReplyReportRepository replyReportRepository;
    @Mock private PlazaLetterRepository letterRepository;
    @Mock private PlazaLetterReplyRepository replyRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("같은 편지의 신고별 처리 메모 조회 성공")
    void 같은_편지의_신고별_처리_메모_조회_성공() {
        // given
        Long letterId = 10L;
        PlazaLetter letter = PlazaLetter.builder()
                .letterId(letterId)
                .content("신고된 편지")
                .build();
        PlazaLetterReport first = createReport(1L, 101L, "첫 번째 메모", letter);
        PlazaLetterReport second = createReport(2L, 102L, "두 번째 메모", letter);
        given(letterReportRepository.findAllByLetterId(letterId)).willReturn(List.of(first, second));

        // when
        PlazaLetterReportDetailResDto result = letterReportAdminService.getReportedLetterDetail(letterId);

        // then
        assertThat(result.reportCount()).isEqualTo(2);
        assertThat(result.reports())
                .extracting(entry -> entry.adminMemo())
                .containsExactly("첫 번째 메모", "두 번째 메모");
    }

    @Test
    @DisplayName("편지 신고 처리 메모 저장 성공")
    void 편지_신고_처리_메모_저장_성공() {
        // given
        Long reportId = 1L;
        PlazaLetterReport report = PlazaLetterReport.builder().reportId(reportId).build();
        given(letterReportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        letterReportAdminService.updateLetterReportMemo(reportId, "처리 완료");

        // then
        assertThat(report.getAdminMemo()).isEqualTo("처리 완료");
    }

    @Test
    @DisplayName("편지 신고 처리 메모 저장 실패 - 신고 내역 없음")
    void 편지_신고_처리_메모_저장_실패_신고_내역_없음() {
        // given
        Long reportId = 999L;
        given(letterReportRepository.findById(reportId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> letterReportAdminService.updateLetterReportMemo(reportId, "메모"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.REPORT_NOT_FOUND);
    }

    private PlazaLetterReport createReport(
            Long reportId, Long reporterId, String adminMemo, PlazaLetter letter) {
        return PlazaLetterReport.builder()
                .reportId(reportId)
                .letter(letter)
                .reporterId(reporterId)
                .reason(ReportReason.OTHER)
                .description("신고 내용")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 7, 1, 12, 0))
                .adminMemo(adminMemo)
                .build();
    }
}
