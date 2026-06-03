package com.example.egobook_be.domain.report.controller;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.letters.service.LetterReportAdminService;
import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.service.AnswerReportAdminService;
import com.example.egobook_be.domain.report.dto.AdminReportMemoReqDto;
import com.example.egobook_be.domain.report.service.AdminReportService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
public class AdminReportController implements AdminReportControllerDocs {

    private final AdminReportService adminReportService;

    @Override
    @GetMapping("/letters")
    public ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReportAdminResDto>>> getReportedLetters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 편지 조회 성공", adminReportService.getReportedLetters(page, size))
        );
    }

    @Override
    @GetMapping("/replies")
    public ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReplyReportAdminResDto>>> getReportedReplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답장 조회 성공", adminReportService.getReportedReplies(page, size))
        );
    }

    @Override
    @GetMapping("/answers")
    public ResponseEntity<GlobalResponse<SliceResponse<AnswerReportAdminResDto>>> getReportedAnswers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답변 조회 성공", adminReportService.getReportedAnswers(page, size))
        );
    }

    @Override
    @GetMapping("/letters/{reportId}")
    public ResponseEntity<GlobalResponse<PlazaLetterReportAdminResDto>> getReportedLetterDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 편지 상세 조회 성공", adminReportService.getReportedLetterDetail(reportId))
        );
    }

    @Override
    @GetMapping("/replies/{reportId}")
    public ResponseEntity<GlobalResponse<PlazaLetterReplyReportAdminResDto>> getReportedReplyDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답장 상세 조회 성공", adminReportService.getReportedReplyDetail(reportId))
        );
    }

    @Override
    @GetMapping("/answers/{reportId}")
    public ResponseEntity<GlobalResponse<AnswerReportAdminResDto>> getReportedAnswerDetail(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("신고된 답변 상세 조회 성공", adminReportService.getReportedAnswerDetail(reportId))
        );
    }

    @Override
    @DeleteMapping("/letters/{letterId}")
    public ResponseEntity<GlobalResponse<Void>> deleteLetter(
            @PathVariable Long letterId
    ) {
        adminReportService.deleteLetter(letterId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 편지 수동 삭제 성공", null));
    }

    @Override
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<GlobalResponse<Void>> deleteReply(
            @PathVariable Long replyId
    ) {
        adminReportService.deleteReply(replyId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 편지 답장 수동 삭제 성공", null));
    }

    @Override
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @PathVariable Long answerId
    ) {
        adminReportService.deleteAnswer(answerId);
        return ResponseEntity.ok(GlobalResponse.success("신고된 오늘의 질문 답변 수동 삭제 성공", null));
    }

    @Override
    @PatchMapping("/letters/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveLetterReport(
            @PathVariable Long reportId
    ) {
        adminReportService.approveLetterReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/letters/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectLetterReport(
            @PathVariable Long reportId
    ) {
        adminReportService.rejectLetterReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 신고 거절 성공", null));
    }

    @Override
    @PatchMapping("/replies/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveReplyReport(
            @PathVariable Long reportId
    ) {
        adminReportService.approveReplyReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 답장 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/replies/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectReplyReport(
            @PathVariable Long reportId
    ) {
        adminReportService.rejectReplyReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("편지 답장 신고 거절 성공", null));
    }

    @Override
    @PatchMapping("/answers/{reportId}/approve")
    public ResponseEntity<GlobalResponse<Void>> approveAnswerReport(
            @PathVariable Long reportId
    ) {
        adminReportService.approveAnswerReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("답변 신고 승인 성공", null));
    }

    @Override
    @PatchMapping("/answers/{reportId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectAnswerReport(
            @PathVariable Long reportId
    ) {
        adminReportService.rejectAnswerReport(reportId);
        return ResponseEntity.ok(GlobalResponse.success("답변 신고 거절 성공", null));
    }

    @Override
    @PatchMapping("/{reportId}/memo")
    public ResponseEntity<GlobalResponse<Void>> setReportMemo(
            @PathVariable Long reportId,
            @RequestBody AdminReportMemoReqDto reqDto
    ){
        adminReportService.setReportMemo(reportId, reqDto);
        return ResponseEntity.ok(GlobalResponse.success("신고 메모 처리 완료", null));
    }
}