package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Question Controller", description = "오늘의 질문 관련 API")
public interface TodayQuestionControllerDocs {

    @Operation(
            summary = "오늘의 질문 조회",
            description = "모든 사용자에게 동일한 오늘의 질문을 조회합니다."
    )
    ResponseEntity<GlobalResponse<TodayQuestionResDto>> getTodayQuestion(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(
            summary = "오늘의 질문 답변 작성",
            description = """
                오늘의 질문에 대한 답변을 작성합니다.
                
                공개 범위 (visibility)
                - PUBLIC : 전체 공개
                - FRIEND : 친구 공개
                - PRIVATE : 비공개
                """
    )
    ResponseEntity<GlobalResponse<Void>> createAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerCreateReqDto reqDto
    );

    @Operation(
            summary = "오늘의 질문 PUBLIC 답변 조회",
            description = "오늘의 질문에 대한 PUBLIC 답변을 조회합니다."
    )
    ResponseEntity<GlobalResponse<SliceResponse<PublicAnswerResDto>>> getPublicAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "답변 수정", description = "오늘의 질문에 대한 자신의 답변을 수정합니다.")
    ResponseEntity<GlobalResponse<Void>> updateAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid AnswerUpdateReqDto reqDto
    );

    @Operation(summary = "친구 답변 조회", description = "오늘의 질문에 대한 친구들의 답변을 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<FriendAnswerResDto>>> getFriendsAnswers(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "내가 작성한 모든 질문 답변 조회", description = "로그인한 사용자가 지금까지 작성한 모든 질문 답변을 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<MyAnswerHistoryResDto>>> getMyAnswerHistory(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(
            summary = "내가 작성한 답변 삭제",
            description = """
                로그인한 사용자가 오늘 작성한 질문 답변을 삭제합니다.
                - 삭제 후 당일 질문에 대한 답변은 다시 작성 가능
                """
    )
    ResponseEntity<GlobalResponse<Void>> deleteAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId
    );

    @Operation(
            summary = "답변 신고",
            description = """
                오늘의 질문에 대한 특정 답변을 신고합니다.
        
                신고 사유 (reason)
                - ABUSE : 비속어 / 욕설 / 모욕적인 표현
                - SPAM : 광고, 도배, 스팸성 답변
                - INAPPROPRIATE : 부적절하거나 불쾌감을 주는 콘텐츠
                - OTHER : 기타 사유
                """
    )
    ResponseEntity<GlobalResponse<AnswerReportResDto>> reportAnswer(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid AnswerReportReqDto reqDto
    );

    @Operation(summary = "신고된 답변 목록 조회 (관리자)", description = "신고된 모든 답변을 조회합니다.")
    ResponseEntity<GlobalResponse<SliceResponse<AnswerReportAdminResDto>>> getReportedAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
