package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.GuestRecertificationReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin Auth Controller", description = "[관리자 인증] 관련 API")
@RequestMapping("/admin/auth")
public interface AdminAuthControllerDocs {

    @Operation(summary = "관리자 Access Token 재발급", description = """
            관리자가 Refresh Token을 이용하여 Access Token을 재발급받는 API입니다.

            [ **입력값** ]
            1. `accessToken`: 만료되었거나 만료되지 않은, 기존에 사용하던 Access Token (Bearer 제외)
               - 기존 Access Token을 Redis 블랙리스트에 등록하기 위해 필요합니다.
            2. `refreshToken`: 만료되지 않은 Refresh Token (Bearer 제외)

            - **기능**: 유효한 Refresh Token을 검증하고, **새로운 Access Token**을 발급합니다.
            - **실패 시**: 401 Unauthorized 반환 → 관리자 계정으로 재로그인이 필요합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공 (Refresh Token은 기존 토큰 그대로 반환)",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Token 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 유효하지 않음 (재로그인 필요)", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/refresh")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> refreshAccessToken(@RequestBody @Valid RefreshReqDto reqDto);

    @Operation(summary = "관리자 Recover Token으로 토큰 재발급", description = """
            관리자의 Refresh Token이 만료되었을 때, 기기에 영구 저장된 Recover Token으로 세션을 복구하는 API입니다.

            [ **입력값** ]
            1. `deviceUid`: 기기 고유 UUID
            2. `accessToken`: 만료되었거나 만료되지 않은, 기존에 사용하던 Access Token (Bearer 제외)
            3. `recoverToken`: 기기에 영구 저장된 Recover Token

            - **기능**:
              1. Recover Token과 서버의 저장값을 대조하여 검증합니다.
              2. 검증 성공 시, **새로운 Access Token, Refresh Token, Recover Token**을 발급합니다.
              3. 새로운 Recover Token이 발급되어 서버에 갱신됩니다. **발급된 Recover Token을 다시 저장하세요.**

            - **실패 시(400)**: Recover Token이 유효하지 않음 → 계정을 삭제 대기 상태로 전환합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access / Refresh / Recover Token이 정상적으로 재발급되었습니다.",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 Recover Token (계정 삭제 대기 처리됨)", content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다. 또는 탈퇴 대기 중인 계정입니다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 인증 정보를 찾을 수 없습니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/recertification")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> recertificationAdminToken(@RequestBody @Valid GuestRecertificationReqDto reqDto);
}
