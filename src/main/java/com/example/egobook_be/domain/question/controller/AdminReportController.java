package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.letters.service.LetterReportAdminService;
import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.service.AnswerReportAdminService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
public class AdminReportController implements AdminReportControllerDocs {

    private final AnswerReportAdminService answerReportAdminService;
    private final LetterReportAdminService letterReportAdminService;

    @Override
    @GetMapping("/letters")
    public ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReportAdminResDto>>> getReportedLetters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 편지 조회 성공", letterReportAdminService.getReportedLetters(page, size))
        );
    }

    @Override
    @GetMapping("/replies")
    public ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReplyReportAdminResDto>>> getReportedReplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답장 조회 성공", letterReportAdminService.getReportedReplies(page, size))
        );
    }

    @Override
    @GetMapping("/answers")
    public ResponseEntity<GlobalResponse<SliceResponse<AnswerReportAdminResDto>>> getReportedAnswers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답변 조회 성공", answerReportAdminService.getReportedAnswers(page, size))
        );
    }

    @Override
    @GetMapping("/letters/{reportId}")
    public ResponseEntity<GlobalResponse<PlazaLetterReportAdminResDto>> getReportedLetterDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 편지 상세 조회 성공", letterReportAdminService.getReportedLetterDetail(reportId))
        );
    }

    @Override
    @GetMapping("/replies/{reportId}")
    public ResponseEntity<GlobalResponse<PlazaLetterReplyReportAdminResDto>> getReportedReplyDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답장 상세 조회 성공", letterReportAdminService.getReportedReplyDetail(reportId))
        );
    }

    @Override
    @GetMapping("/answers/{reportId}")
    public ResponseEntity<GlobalResponse<AnswerReportAdminResDto>> getReportedAnswerDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답변 상세 조회 성공", answerReportAdminService.getReportedAnswerDetail(reportId))
        );
    }

    @Override
    @DeleteMapping("/letters/{letterId}")
    public ResponseEntity<GlobalResponse<Void>> deleteLetter(
            @PathVariable Long letterId
    ) {
        letterReportAdminService.deleteLetter(letterId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 편지 수동 삭제 성공", null));
    }

    @Override
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<GlobalResponse<Void>> deleteReply(
            @PathVariable Long replyId
    ) {
        letterReportAdminService.deleteReply(replyId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 편지 답장 수동 삭제 성공", null));
    }

    @Override
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @PathVariable Long answerId
    ) {
        answerReportAdminService.deleteAnswer(answerId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 오늘의 질문 답변 수동 삭제 성공", null));
    }

    @Override
    @PatchMapping("/letters/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveLetterReport(@PathVariable Long reportId) {
        letterReportAdminService.approveLetterReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/letters/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectLetterReport(@PathVariable Long reportId) {
        letterReportAdminService.rejectLetterReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 신고 거절 성공", null));
    }

    @Override
    @PatchMapping("/replies/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveReplyReport(@PathVariable Long reportId) {
        letterReportAdminService.approveReplyReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 답장 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/replies/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectReplyReport(@PathVariable Long reportId) {
        letterReportAdminService.rejectReplyReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 답장 신고 거절 성공", null));
    }

    @Override
    @PatchMapping("/answers/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveAnswerReport(@PathVariable Long reportId) {
        answerReportAdminService.approveReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("답변 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/answers/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectAnswerReport(@PathVariable Long reportId) {
        answerReportAdminService.rejectReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("답변 신고 거절 성공", null));
    }
}