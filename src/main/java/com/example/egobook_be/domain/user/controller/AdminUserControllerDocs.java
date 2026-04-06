package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin User Controller", description = "회원관리 관리자 API")
@RequestMapping("/admin/users")
public interface AdminUserControllerDocs {
    @Operation(summary = "회원 리스트 검색", description = """
            키워드 & 필터 검색을 통해 회원들을 리스트로 검색하는 API입니다.
            
            [**Query Parameter**]
            - page: 페이지 번호 (1 ~ n)
            - size: 페이지 크기
            - keyword: 검색창에 작성한 검색 키워드
                (검색할 수 있는 요소= ```Account Code``` & ```Email``` & ```Nickname```)
            - status: 검색할 사용자의 상태 필터
                (필터링할 수 있는 요소)
                    1. ```ACTIVE``` (활동 상태)
                    2. ```DORMANT``` (휴면 상태)
                    3. ```WITHDRAW_PENDING``` (탈퇴 대기 상태)
                    4. ```SUSPENDED``` (정지 상태)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 리스트 검색 성공",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 필터 값을 보냈습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 키워드 및 필터에 맞는 사용자 정보들을 찾지 못했습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    ResponseEntity<GlobalResponse<SliceResponse<SearchUserResDto>>> searchUserList(
        @Parameter(description = "검색 키워드 (AccountCode | Email | Nickname", required = true)
        @RequestParam("keyword") String keyword,

        @Parameter(description = "사용자 상태 필터 키워드 (ACTIVE | DORMANT | WITHDRAW_PENDING | SUSPENDED)")
        @RequestParam("status") UserStatus status,

        @Parameter(description = "Page 번호 (1 ~ N)", required = true)
        @RequestParam(value = "page", defaultValue = "1") Integer page,

        @Parameter(description = "Page 크기", required = true)
        @RequestParam(value = "size", defaultValue = "5") Integer size
    );
}
