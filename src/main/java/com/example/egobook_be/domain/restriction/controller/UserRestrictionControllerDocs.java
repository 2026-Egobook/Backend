package com.example.egobook_be.domain.restriction.controller;

import com.example.egobook_be.domain.restriction.dto.MyRestrictionResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// 사용자용 제재 조회 API 문서
@Tag(name = "Restriction Controller", description = "사용자 제재 상태 조회 API")
@RequestMapping("/restrictions")
public interface UserRestrictionControllerDocs {

    @Operation(summary = "내 제재 상태 조회", description = """
            로그인한 사용자의 기능별 제재 상태를 조회하는 API입니다.
            제한 안내 팝업("N일간 이용이 제한됩니다")에 필요한 정보를 한 번에 내려줍니다.

            [**Response**]
            - letter: 편지 기능 제재 상태
            - questionAnswer: 오늘의 질문 답변 기능 제재 상태

            [**각 항목의 필드**]
            - restricted: 현재 제재 중인지 여부
            - reason: 팝업에 그대로 표시할 제재 사유
                (신고 3회 누적 자동 제재: `커뮤니티 이용 규칙 위반 (반복된 신고 접수)`, 관리자 제재: 관리자가 입력한 사유)
            - restrictedUntil: 제재 종료 시각 (한국 시간). 이 시각부터 다시 이용할 수 있습니다.

            제재 중이 아니면 restricted=false 이고 reason, restrictedUntil 은 null 입니다.
            신고 3회 누적 자동 제재는 편지와 질문 답변 기능에 동시에 적용됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 제재 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = MyRestrictionResDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    ResponseEntity<GlobalResponse<MyRestrictionResDto>> getMyRestrictions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );
}
