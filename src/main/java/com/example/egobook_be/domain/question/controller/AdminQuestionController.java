package com.example.egobook_be.domain.question.controller;

import com.example.egobook_be.domain.question.dto.AdminQuestionReqDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionResDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionListResDto;
import com.example.egobook_be.domain.question.service.AdminQuestionService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/question")
@Tag(name = "Admin Question Controller", description = "관리자 [질문] 관련 API")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    @Operation(
            summary = "질문 추가",
            description = "[내용, 날짜] 정보를 입력해 질문을 추가합니다."
    )
    @PostMapping
    public GlobalResponse<AdminQuestionResDto> createQuestion(@RequestBody AdminQuestionReqDto reqDto) {
        return GlobalResponse.success("질문 등록 성공", adminQuestionService.createQuestion(reqDto));
    }

    @Operation(
            summary = "질문 목록 조회",
            description = "날짜 내림차순으로 한 페이지에 20개씩 질문 목록을 조회합니다. page: 1~"
    )
    @GetMapping
    public GlobalResponse<AdminQuestionListResDto> getQuestionList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return GlobalResponse.success("질문 목록 조회 성공", adminQuestionService.getQuestionList(page, size));
    }

    @Operation(
            summary = "질문 단건 조회",
            description = "id값으로 질문 한 건을 상세 조회합니다. 삭제하기 전의 질문은 deleted_at이 null, 삭제 후에는 삭제 시간이 저장됩니다."
    )
    @GetMapping("/{questionId}")
    public GlobalResponse<AdminQuestionResDto> getQuestionDetail(@PathVariable Long questionId) {
        return GlobalResponse.success("질문 단건 조회 성공", adminQuestionService.getQuestionDetail(questionId));
    }

    @Operation(
            summary = "질문 수정",
            description = "질문을 수정합니다. [내용,날짜] 중 하나만 수정하더라도 나머지 요소의 내용도 함께 입력하여 요청을 보내야 합니다."
    )
    @PutMapping("/{questionId}")
    public GlobalResponse<AdminQuestionResDto> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody AdminQuestionReqDto reqDto) {
        return GlobalResponse.success("질문 수정 성공", adminQuestionService.updateQuestion(questionId, reqDto));
    }

    @Operation(
            summary = "질문 삭제",
            description = "id값으로 질문을 삭제합니다."
    )
    @DeleteMapping("/{questionId}")
    public GlobalResponse<Void> deleteQuestion(@PathVariable Long questionId) {
        adminQuestionService.deleteQuestion(questionId);
        return GlobalResponse.success("질문 삭제 성공", null);
    }
}