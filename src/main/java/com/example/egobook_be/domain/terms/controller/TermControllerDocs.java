package com.example.egobook_be.domain.terms.controller;

import com.example.egobook_be.domain.terms.dto.TermResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Terms Controller", description = "약관(이용약관, 개인정보 처리방침 등) 관련 API")
@RequestMapping("/terms")
public interface TermControllerDocs {
    @Operation(
            summary = "전체 이용 약관 목록 조회",
            description = "회원가입 시 사용자에게 보여줄 **최신 버전의 모든 약관 목록**을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "요청 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TermResDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    @GetMapping("")
    ResponseEntity<GlobalResponse<List<TermResDto>>> getTerms();
}
