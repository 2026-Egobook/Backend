package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReplyReportAdminResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterReportAdminResDto;
import com.example.egobook_be.domain.question.dto.AnswerReportAdminResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin Report", description = "신고 조회 관리자 API")
public interface AdminReportControllerDocs {

    @Operation(summary = "[관리자] 신고된 편지 목록 조회", description = "신고된 편지 목록을 최신순으로 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReportAdminResDto>>> getReportedLetters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "[관리자] 신고된 편지 상세 조회", description = "신고된 편지 1건을 상세 조회합니다.")
    ResponseEntity<GlobalResponse<PlazaLetterReportAdminResDto>> getReportedLetterDetail(
            @PathVariable Long reportId
    );

    @Operation(summary = "[관리자] 신고된 편지 답장 목록 조회", description = "신고된 편지 답장 목록을 최신순으로 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<PlazaLetterReplyReportAdminResDto>>> getReportedReplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "[관리자] 신고된 편지 답장 상세 조회", description = "신고된 편지 답장 1건을 상세 조회합니다.")
    ResponseEntity<GlobalResponse<PlazaLetterReplyReportAdminResDto>> getReportedReplyDetail(
            @PathVariable Long reportId
    );

    @Operation(summary = "[관리자] 신고된 오늘의 질문 답변 목록 조회", description = "신고된 오늘의 질문 답변 목록을 최신순으로 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<AnswerReportAdminResDto>>> getReportedAnswers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "[관리자] 오늘의 질문 답변 상세 조회", description = "신고된 오늘의 질문 답변 1건을 상세 조회합니다.")
    ResponseEntity<GlobalResponse<AnswerReportAdminResDto>> getReportedAnswerDetail(
            @PathVariable Long reportId
    );

    @Operation(summary = "[관리자] 편지 수동 삭제", description = "편지를 수동으로 삭제합니다.")
    ResponseEntity<GlobalResponse<Void>> deleteLetter(
            @PathVariable Long letterId
    );

    @Operation(summary = "[관리자] 편지 답장 수동 삭제", description = "편지 답장을 수동으로 삭제합니다.")
    ResponseEntity<GlobalResponse<Void>> deleteReply(
            @PathVariable Long replyId
    );

    @Operation(summary = "[관리자] 오늘의 질문 답변 수동 삭제", description = "오늘의 질문 답변을 수동으로 삭제합니다.")
    ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @PathVariable Long answerId
    );
}
