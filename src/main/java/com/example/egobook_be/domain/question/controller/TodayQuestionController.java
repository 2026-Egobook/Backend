package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.service.AnswerReportService;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Question Controller", description = "오늘의 질문 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class TodayQuestionController {

    private final TodayQuestionService todayQuestionService;
    private final AnswerReportService answerReportService;


    @Operation(
            summary = "오늘의 질문 조회",
            description = "모든 사용자에게 동일한 오늘의 질문을 조회합니다."
    )
    @GetMapping("/today")
    public ResponseEntity<GlobalResponse<TodayQuestionResDto>> getTodayQuestion(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "오늘의 질문 조회 성공",
                        todayQuestionService.getTodayQuestion(userId)
                )
        );
    }

    @Operation(
            summary = "오늘의 질문 답변 작성",
            description = """
                오늘의 질문에 대한 답변을 작성합니다.
                
                공개 범위:
                - PUBLIC : 전체 공개
                - FRIEND : 친구 공개
                - PRIVATE : 비공개
                """
    )
    @PostMapping("/answers")
    public ResponseEntity<GlobalResponse<Void>> createAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerCreateReqDto reqDto
    ) {
        todayQuestionService.createAnswer(userId, reqDto);
        return ResponseEntity.ok(
                GlobalResponse.success("답변 작성 완료", null)
        );
    }


    @Operation(
            summary = "오늘의 질문 PUBLIC 답변 조회",
            description = """
                오늘의 질문에 대한 PUBLIC 답변을 조회합니다.
                """
    )
    @GetMapping("/answers/all")
    public ResponseEntity<GlobalResponse<SliceResponse<PublicAnswerResDto>>> getPublicAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "오늘의 질문 PUBLIC 답변 조회 성공",
                        todayQuestionService.getPublicAnswers(page, size)
                )
        );
    }
//    public ResponseEntity<GlobalResponse<List<PublicAnswerResDto>>> getPublicAnswers() {
//        return ResponseEntity.ok(
//                GlobalResponse.success(
//                        "오늘의 질문 PUBLIC 답변 조회 성공",
//                        todayQuestionService.getPublicAnswers()
//                )
//        );
//    }

    @Operation(
            summary = "답변 수정",
            description = """
                오늘의 질문에 대한 자신의 답변을 수정합니다.
                """
    )
    @PutMapping("/answers")
    public ResponseEntity<GlobalResponse<Void>> updateAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerUpdateReqDto reqDto
    ) {
        todayQuestionService.updateAnswer(userId, reqDto);
        return ResponseEntity.ok(
                GlobalResponse.success("답변 수정 완료", null)
        );
    }

    @Operation(
            summary = "친구 답변 조회",
            description = """
                오늘의 질문에 대한 친구들의 답변을 조회합니다.
                """
    )
    @GetMapping("/answers/friends")
    public ResponseEntity<GlobalResponse<SliceResponse<FriendAnswerResDto>>> getFriendsAnswers(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "친구 공개 답변 조회 성공",
                        todayQuestionService.getFriendsAnswers(userId, page, size)
                )
        );
    }

    @Operation(
            summary = "내가 작성한 모든 질문 답변 조회",
            description = """
                로그인한 사용자가 지금까지 작성한 모든 질문 답변을 조회합니다.
                """
    )
    @GetMapping("/answers/me/history")
    public ResponseEntity<GlobalResponse<SliceResponse<MyAnswerHistoryResDto>>> getMyAnswerHistory(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "내 답변 전체 조회 성공",
                        todayQuestionService.getMyAnswerHistory(userId, page, size)
                )
        );
    }

    @Operation(
            summary = "내가 작성한 답변 삭제",
            description = """
                로그인한 사용자가 오늘 작성한 질문 답변을 삭제합니다.

                - 삭제 후 당일 질문에 대한 답변은 다시 작성 가능
                """
    )
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId
    ) {
        todayQuestionService.deleteAnswer(userId, answerId);
        return ResponseEntity.ok(
                GlobalResponse.success("답변 삭제 성공", null)
        );
    }

    @Operation(
            summary = "답변 신고",
            description = "오늘의 질문에 대한 특정 답변을 신고합니다."
    )
    @PostMapping("/answers/{answerId}/report")
    public ResponseEntity<GlobalResponse<AnswerReportResDto>> reportAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid AnswerReportReqDto reqDto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "답변 신고 완료",
                        answerReportService.reportAnswer(userId, answerId, reqDto)
                )
        );
    }
}
