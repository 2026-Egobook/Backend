package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.enums.LetterReportReason;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.LetterReportService;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LetterReportServiceTest {

    @InjectMocks
    private LetterReportService letterReportService;

    @Mock
    private PlazaLetterReportRepository letterReportRepository;

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Test
    @DisplayName("reportLetter_OTHER 사유인데 설명이 없으면 예외가 발생한다")
    void reportLetter_otherReasonWithoutDescription_fail() {
        assertThatThrownBy(() -> letterReportService.reportLetter(1L, 10L, LetterReportReason.OTHER, " "))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.INVALID_REPORT_REASON);
    }

    @Test
    @DisplayName("reportLetter_이미 신고한 편지면 예외가 발생한다")
    void reportLetter_alreadyReported_fail() {
        given(letterReportRepository.existsByLetter_LetterIdAndReporterId(1L, 10L)).willReturn(true); // letterId=1L, userId=10L

        assertThatThrownBy(() -> letterReportService.reportLetter(1L, 10L, LetterReportReason.ABUSE, "신고"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.ALREADY_REPORTED);

        verify(plazaLetterRepository, never()).findById(any());
    }

    @Test
    @DisplayName("reportLetter_받은 편지가 아니면 예외가 발생한다")
    void reportLetter_notReceiver_fail() {
        PlazaLetter letter = plazaLetter(1L, 2L, 3L); // letterId=1L, senderId=2L, receiverId=3L (userId=10L이 아님)
        given(letterReportRepository.existsByLetter_LetterIdAndReporterId(1L, 10L)).willReturn(false);
        given(plazaLetterRepository.findById(1L)).willReturn(Optional.of(letter));

        assertThatThrownBy(() -> letterReportService.reportLetter(1L, 10L, LetterReportReason.ABUSE, "신고"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("reportLetter_누적 신고가 3회 이상이면 편지를 삭제한다")
    void reportLetter_reportCountThreeOrMore_deleteLetter() {
        PlazaLetter letter = plazaLetter(1L, 2L, 10L); // letterId=1L, receiverId=10L (userId와 일치)
        given(letterReportRepository.existsByLetter_LetterIdAndReporterId(1L, 10L)).willReturn(false);
        given(plazaLetterRepository.findById(1L)).willReturn(Optional.of(letter));
        given(letterReportRepository.countByLetter_LetterId(1L)).willReturn(3L);

        letterReportService.reportLetter(1L, 10L, LetterReportReason.ABUSE, "신고 사유");

        verify(letterReportRepository).save(any(PlazaLetterReport.class));
        verify(plazaLetterRepository).deleteById(1L);
    }

    @Test
    @DisplayName("reportLetter_정상 신고면 신고 내역을 저장한다")
    void reportLetter_validRequest_success() {
        PlazaLetter letter = plazaLetter(1L, 2L, 10L); // letterId=1L, receiverId=10L (userId와 일치)
        given(letterReportRepository.existsByLetter_LetterIdAndReporterId(1L, 10L)).willReturn(false);
        given(plazaLetterRepository.findById(1L)).willReturn(Optional.of(letter));
        given(letterReportRepository.countByLetter_LetterId(1L)).willReturn(1L);

        letterReportService.reportLetter(1L, 10L, LetterReportReason.SPAM, "광고 같아요");

        verify(letterReportRepository).save(any(PlazaLetterReport.class));
        verify(plazaLetterRepository, never()).deleteById(any());
    }

    private PlazaLetter plazaLetter(Long letterId, Long senderId, Long receiverId) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .threadId(100L)
                .senderId(senderId)
                .receiverId(receiverId)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("내용")
                .status(PlazaLetterStatus.ARRIVED)
                .createdAt(LocalDateTime.now().minusHours(1))
                .arrivedAt(LocalDateTime.now().minusMinutes(30))
                .replyDeadlineAt(LocalDateTime.now().plusHours(23))
                .build();
    }
}