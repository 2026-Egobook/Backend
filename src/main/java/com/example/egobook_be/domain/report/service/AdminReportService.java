package com.example.egobook_be.domain.report.service;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.letters.service.LetterReportAdminService;
import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.domain.question.service.AnswerReportAdminService;
import com.example.egobook_be.domain.report.dto.AdminReportMemoReqDto;
import com.example.egobook_be.domain.report.enums.ReportErrorCode;
import com.example.egobook_be.domain.report.enums.ReportMemoType;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {
    private final AnswerReportAdminService answerReportAdminService;
    private final LetterReportAdminService letterReportAdminService;

    public SliceResponse<PlazaLetterReportAdminResDto> getReportedLetters(int page, int size) {
        return letterReportAdminService.getReportedLetters(page, size);
    }

    public SliceResponse<PlazaLetterReplyReportAdminResDto> getReportedReplies(int page, int size) {
        return letterReportAdminService.getReportedReplies(page, size);
    }

    public SliceResponse<AnswerReportAdminResDto> getReportedAnswers(
            int page,
            int size
    ){
        return answerReportAdminService.getReportedAnswers(page, size);
    }

    public PlazaLetterReportAdminResDto getReportedLetterDetail(Long reportId) {
        return letterReportAdminService.getReportedLetterDetail(reportId);
    }

    public PlazaLetterReplyReportAdminResDto getReportedReplyDetail(Long reportId) {
        return letterReportAdminService.getReportedReplyDetail(reportId);
    }

    public AnswerReportAdminResDto getReportedAnswerDetail(Long reportId) {
        return answerReportAdminService.getReportedAnswerDetail(reportId);
    }

    public void deleteLetter(Long letterId) {
        letterReportAdminService.deleteLetter(letterId);
    }

    public void deleteReply(Long replyId) {
        letterReportAdminService.deleteReply(replyId);
    }

    public void deleteAnswer(Long answerId) {
        answerReportAdminService.deleteAnswer(answerId);
    }

    public void approveLetterReport(Long reportId) {
        letterReportAdminService.approveLetterReport(reportId);
    }

    public void rejectLetterReport(Long reportId) {
        letterReportAdminService.rejectLetterReport(reportId);
    }

    public void approveReplyReport(Long reportId) {
        letterReportAdminService.approveReplyReport(reportId);
    }

    public void rejectReplyReport(Long reportId) {
        letterReportAdminService.rejectReplyReport(reportId);
    }

    public void approveAnswerReport(Long reportId) {
        answerReportAdminService.approveAnswerReport(reportId);
    }

    public void rejectAnswerReport(Long reportId) {
        answerReportAdminService.rejectAnswerReport(reportId);
    }

    /**
     * 신고 메모를 작성
     * @param reportId : 신고 처리 메모를 작성할 PK
     * @param reqDto : 요청 DTO
     */
    public void setReportMemo(Long reportId, AdminReportMemoReqDto reqDto) {
        log.debug("[AdminReportService] setReportMemo() START - reportId:{}", reportId);
        switch (reqDto.reportMemoType()){
            case ReportMemoType.LETTER -> {
                letterReportAdminService.updateLetterReportMemo(reportId, reqDto.adminMemo());
                log.debug("[AdminReportService] setReportMemo() END, 편지 신고 처리 메모 작성 완료 - reportId:{}", reportId);
            }
            case ReportMemoType.LETTER_REPLY -> {
                letterReportAdminService.updateLetterReplyReportMemo(reportId, reqDto.adminMemo());
                log.debug("[AdminReportService] setReportMemo() END, 편지 답변 신고 처리 메모 작성 완료 - reportId:{}", reportId);
            }
            case ReportMemoType.ANSWER -> {
                answerReportAdminService.updateAnswerReportMemo(reportId, reqDto.adminMemo());
                log.debug("[AdminReportService] setReportMemo() END, 오늘의 질문 신고 처리 메모 작성 완료 - reportId:{}", reportId);
            }
            default -> {
                log.error("[AdminReportService] setReportMemo() 유효하지 않은 신고 메모 타입으로 신고 처리 메모 작성 시도 - reportId:{}", reportId);
                throw new CustomException(ReportErrorCode.INVALID_REPORT_MEMO_TYPE);
            }
        }
    }

}
