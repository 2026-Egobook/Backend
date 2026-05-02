package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.AdminAuthReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.AdminLoginResDto;
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

    @Operation(summary = "관리자 회원가입", description = """
            관리자가 아이디와 비밀번호를 입력하여 신규 계정을 등록하는 API입니다.

            [ **입력값** ]
            1. `adminId`: 관리자 아이디
            2. `password`: 관리자 비밀번호

            - **기능**: 신규 관리자 계정을 생성합니다.
            - **실패 시**:
              - 400: 필수 값 누락
              - 409: 이미 존재하는 관리자 아이디

            - **주의사항** -
                - 회원가입을 한다 해서 바로 로그인할 수 없습니다.
                - 직접 해당 계정의 설정을 변경해야 로그인이 가능합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (아이디/비밀번호 누락)", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 관리자 아이디", content = @Content)
    })
    @PostMapping("/register")
    ResponseEntity<GlobalResponse<Void>> registerAdmin(@RequestBody @Valid AdminAuthReqDto reqDto);

    @Operation(summary = "관리자 로그인", description = """
            관리자가 아이디와 비밀번호를 입력하여 로그인하는 API입니다.

            [ **입력값** ]
            1. `adminId`: 관리자 아이디
            2. `password`: 관리자 비밀번호

            - **기능**: 아이디/비밀번호 검증 후 Access Token, Refresh Token을 발급합니다.
            - **실패 시**:
              - 400: 필수 값 누락
              - 401: 아이디 또는 비밀번호 불일치
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AdminLoginResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (아이디/비밀번호 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치", content = @Content)
    })
    @PostMapping("/login")
    ResponseEntity<GlobalResponse<AdminLoginResDto>> loginAdmin(@RequestBody @Valid AdminAuthReqDto reqDto);


    @Operation(summary = "관리자 Access Token 재발급", description = """
            관리자가 Refresh Token을 이용하여 Access Token을 재발급받는 API입니다.

            [ **입력값** ]
            1. `accessToken`: 만료되었거나 만료되지 않은, 기존에 사용하던 Access Token (Bearer 제외)
               - 기존 Access Token을 Redis 블랙리스트에 등록하기 위해 필요합니다.
            2. `refreshToken`: 만료되지 않은 Refresh Token (Bearer 제외)

            - **기능**: 유효한 Refresh Token을 검증하고, **새로운 Access Token**을 발급합니다.
            - **실패 시**: Redis에 Refresh Token 정보가 없으면 401 Unauthorized 반환 → 관리자 계정으로 재로그인이 필요합니다.
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
}
