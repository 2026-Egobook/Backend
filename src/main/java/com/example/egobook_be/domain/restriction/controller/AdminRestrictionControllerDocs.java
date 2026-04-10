package com.example.egobook_be.domain.restriction.controller;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin Restriction Controller", description = "사용자 제재 관리 관리자 API")
@RequestMapping("/admin/users")
public interface AdminRestrictionControllerDocs {

    @Operation(summary = "사용자 제재 적용", description = """
            특정 사용자에게 7일간 제재를 적용하는 API입니다.

            [**Path Variable**]
            - userId: 제재를 적용할 ```ROLE_USER``` 권한을 가진 사용자의 ID

            [**Request Body**]
            - ```domainType```: 제재할 서비스의 도메인 타입
                1. ```LETTER``` (편지)
                2. ```QUESTION_ANSWER``` (오늘의 질문 답장)
            - ```reason```: 제재 사유
                1. ```ABUSE``` (비속어/욕설/모욕)
                2. ```SPAM``` (광고/스팸)
                3. ```INAPPROPRIATE``` (부적절한 콘텐츠)
                4. ```OTHER``` (기타)
            - ```description```: 제재 사유 상세 설명 (최대 500자)

            [**반환 정보**]
            - ```restrictionId```: 생성된 Restriction 도메인 PK
            - ```restrictionStatus```: 제재 상태
                - ```ACTIVE``` (활성화됨)
                - ```CANCELED``` (관리자에 의해 취소됨)
            - ```restrictionUntil```: 제재 종료 예정 시간 (제재 적용 시각으로부터 **7일 뒤**)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "사용자 제재 적용 성공",
                    content = @Content(schema = @Schema(implementation = RestrictionCreateResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값입니다. (필수 필드 누락 또는 유효하지 않은 enum 값)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/restrictions")
    ResponseEntity<GlobalResponse<RestrictionCreateResDto>> createRestriction(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long adminId,

            @Parameter(description = "제재할 대상 사용자 ID", required = true)
            @PathVariable Long userId,

            @RequestBody @Valid RestrictionCreateReqDto reqDto
    );
}
