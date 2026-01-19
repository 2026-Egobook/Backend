package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.question.dto.AnswerCreateReqDto;
import com.example.egobook_be.domain.question.dto.PublicAnswerResDto;
import com.example.egobook_be.domain.question.dto.TodayQuestionResDto;
import com.example.egobook_be.domain.question.service.TodayQuestionService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Question API", description = "오늘의 질문 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class TodayQuestionController {

    private final TodayQuestionService todayQuestionService;

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
    public ResponseEntity<GlobalResponse<List<PublicAnswerResDto>>> getPublicAnswers() {
        return ResponseEntity.ok(
                GlobalResponse.success(
                        "오늘의 질문 PUBLIC 답변 조회 성공",
                        todayQuestionService.getPublicAnswers()
                )
        );
    }
}
