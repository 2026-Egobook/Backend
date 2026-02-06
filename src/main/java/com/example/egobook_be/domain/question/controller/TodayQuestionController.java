package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.service.AnswerReportAdminService;
import com.example.egobook_be.domain.question.service.AnswerReportService;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class TodayQuestionController implements TodayQuestionControllerDocs {

    private final TodayQuestionService todayQuestionService;
    private final AnswerReportService answerReportService;
    private final AnswerReportAdminService answerReportAdminService;

    @GetMapping("/today")
    public ResponseEntity<GlobalResponse<TodayQuestionResDto>> getTodayQuestion(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(GlobalResponse.success("오늘의 질문 조회 성공", todayQuestionService.getTodayQuestion(userId)));
    }

    @PostMapping("/answers")
    public ResponseEntity<GlobalResponse<Void>> createAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerCreateReqDto reqDto
    ) {
        todayQuestionService.createAnswer(userId, reqDto);
        return ResponseEntity.ok(GlobalResponse.success("답변 작성 완료", null));
    }

    @GetMapping("/answers/all")
    public ResponseEntity<GlobalResponse<SliceResponse<PublicAnswerResDto>>> getPublicAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(GlobalResponse.success("오늘의 질문 PUBLIC 답변 조회 성공", todayQuestionService.getPublicAnswers(page, size)));
    }

    @PutMapping("/answers")
    public ResponseEntity<GlobalResponse<Void>> updateAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerUpdateReqDto reqDto
    ) {
        todayQuestionService.updateAnswer(userId, reqDto);
        return ResponseEntity.ok(GlobalResponse.success("답변 수정 완료", null));
    }

    @GetMapping("/answers/friends")
    public ResponseEntity<GlobalResponse<SliceResponse<FriendAnswerResDto>>> getFriendsAnswers(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(GlobalResponse.success("친구 공개 답변 조회 성공", todayQuestionService.getFriendsAnswers(userId, page, size)));
    }

    @GetMapping("/answers/me/history")
    public ResponseEntity<GlobalResponse<SliceResponse<MyAnswerHistoryResDto>>> getMyAnswerHistory(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(GlobalResponse.success("내 답변 전체 조회 성공", todayQuestionService.getMyAnswerHistory(userId, page, size)));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId
    ) {
        todayQuestionService.deleteAnswer(userId, answerId);
        return ResponseEntity.ok(GlobalResponse.success("답변 삭제 성공", null));
    }

    @PostMapping("/answers/{answerId}/report")
    public ResponseEntity<GlobalResponse<AnswerReportResDto>> reportAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid AnswerReportReqDto reqDto
    ) {
        return ResponseEntity.ok(GlobalResponse.success("답변 신고 완료", answerReportService.reportAnswer(userId, answerId, reqDto)));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<SliceResponse<AnswerReportAdminResDto>>> getReportedAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(GlobalResponse.success("신고된 답변 조회 성공", answerReportAdminService.getReportedAnswers(page, size)));
    }
}
