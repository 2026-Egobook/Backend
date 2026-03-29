package com.example.egobook_be.domain.letter;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.service.ReplyReportService;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReplyReportServiceTest {

    @InjectMocks
    private ReplyReportService replyReportService;

    @Mock
    private PlazaLetterReplyReportRepository replyReportRepository;

    @Mock
    private PlazaLetterReplyRepository plazaLetterReplyRepository;

    @Mock
    private PlazaLetterRepository plazaLetterRepository;

    @Test
    @DisplayName("reportReply_OTHER 사유인데 설명이 없으면 예외가 발생한다")
    void reportReply_otherReasonWithoutDescription_fail() {
        assertThatThrownBy(() -> replyReportService.reportReply(1L, 10L, ReplyReportReason.OTHER, " "))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.INVALID_REPORT_REASON);
    }

    @Test
    @DisplayName("reportReply_이미 신고한 답장이면 예외가 발생한다")
    void reportReply_alreadyReported_fail() {
        given(replyReportRepository.existsByReply_ReplyIdAndReporterId(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> replyReportService.reportReply(1L, 10L, ReplyReportReason.ABUSE, "신고"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.ALREADY_REPORTED);

        verify(plazaLetterReplyRepository, never()).findByIdWithLetter(any());
    }

    @Test
    @DisplayName("reportReply_수신자는 답장을 신고할 수 없다")
    void reportReply_receiverReports_fail() {
        PlazaLetter letter = plazaLetter(100L, 2L, 1L);
        PlazaLetterReply reply = plazaReply(10L, letter, 2L);

        given(replyReportRepository.existsByReply_ReplyIdAndReporterId(10L, 1L)).willReturn(false);
        given(plazaLetterReplyRepository.findByIdWithLetter(10L)).willReturn(Optional.of(reply));

        assertThatThrownBy(() -> replyReportService.reportReply(1L, 10L, ReplyReportReason.ABUSE, "신고"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(LettersErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("reportReply_누적 신고가 3회 이상이면 답장을 삭제 처리한다")
    void reportReply_reportCountThreeOrMore_deleteReply() {
        PlazaLetter letter = plazaLetter(100L, 1L, 2L);
        PlazaLetterReply reply = plazaReply(10L, letter, 2L);

        given(replyReportRepository.existsByReply_ReplyIdAndReporterId(10L, 1L)).willReturn(false);
        given(plazaLetterReplyRepository.findByIdWithLetter(10L)).willReturn(Optional.of(reply));
        given(replyReportRepository.countByReply_ReplyId(10L)).willReturn(3L);

        replyReportService.reportReply(1L, 10L, ReplyReportReason.ABUSE, "신고");

        verify(replyReportRepository).save(any(PlazaLetterReplyReport.class));
        verify(replyReportRepository).moveReplyToReportDbAndDelete(10L, PlazaLetterReply.ReplyStatus.DELETED);
        verify(plazaLetterReplyRepository).deleteById(10L);
    }

    @Test
    @DisplayName("reportReply_정상 신고면 신고 내역만 저장한다")
    void reportReply_validRequest_success() {
        PlazaLetter letter = plazaLetter(100L, 1L, 2L);
        PlazaLetterReply reply = plazaReply(10L, letter, 2L);

        given(replyReportRepository.existsByReply_ReplyIdAndReporterId(10L, 1L)).willReturn(false);
        given(plazaLetterReplyRepository.findByIdWithLetter(10L)).willReturn(Optional.of(reply));
        given(replyReportRepository.countByReply_ReplyId(10L)).willReturn(1L);

        replyReportService.reportReply(1L, 10L, ReplyReportReason.SPAM, "도배 답장");

        verify(replyReportRepository).save(any(PlazaLetterReplyReport.class));
        verify(replyReportRepository, never()).moveReplyToReportDbAndDelete(any(), any());
        verify(plazaLetterReplyRepository, never()).deleteById(any());
    }

    private PlazaLetter plazaLetter(Long letterId, Long senderId, Long receiverId) {
        return PlazaLetter.builder()
                .letterId(letterId)
                .threadId(200L)
                .senderId(senderId)
                .receiverId(receiverId)
                .mode(PlazaLetterMode.RANDOM)
                .fromLabel("익명")
                .content("편지")
                .status(PlazaLetterStatus.ARRIVED)
                .createdAt(LocalDateTime.now().minusHours(10))
                .arrivedAt(LocalDateTime.now().minusHours(9))
                .replyDeadlineAt(LocalDateTime.now().plusHours(15))
                .build();
    }

    private PlazaLetterReply plazaReply(Long replyId, PlazaLetter letter, Long replierId) {
        return PlazaLetterReply.builder()
                .replyId(replyId)
                .threadId(letter.getThreadId())
                .letter(letter)
                .replierId(replierId)
                .text("답장")
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now())
                .status(PlazaLetterReply.ReplyStatus.SENT)
                .build();
    }
}
