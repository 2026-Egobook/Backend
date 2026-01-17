package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.req.GuestRefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Tag(name = "Auth Controller", description = "[인증/인가] 관련 API")
@RequestMapping("/auth")
public interface AuthControllerDocs {
    @Operation(summary = "Guest 회원가입 (기기 최초 등록)", description = """
            사용자가 앱을 처음 설치하고 실행했을 때 호출되는 API입니다.
            
            - **기능**: 기기 고유 ID(UUID)를 등록하고, **초기 토큰 3종(Access, Refresh, Recover)**을 발급합니다.
            - **주의**: 
              1. 이미 등록된 기기라면 에러가 발생합니다. (로그인 API 사용 권장)
              2. 등록된 기기임을 클라이언트에서 확인할 때, recoverToken의 여부를 확인하세요. (recoverToken이 없다면 처음 로그인 한 것임)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공 및 토큰 발급 완료",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (기기 UID 누락 등)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 등록된 기기 (중복 가입 시도)",
                    content = @Content)
    })
    @PostMapping("/guest/join")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> guestJoin(@RequestBody @Valid GuestJoinReqDto reqDto);

    @Operation(summary = "Guest Access Token 재발급", description = """
            Access Token이 만료되었을 때, Refresh Token을 이용하여 토큰을 갱신합니다.
            
            - **기능**: 유효한 Refresh Token을 검증하고, **새로운 Access Token과 Refresh Token**을 발급합니다. (RTR 적용)
            - **실패 시**: 401 Unauthorized 리턴 -> 클라이언트는 [Guest 복구(Recertification)] API를 호출해야 합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공 (Access/Refresh 모두 재발급)",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Token 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 유효하지 않음 (재로그인 필요)", content = @Content)
    })
    @PostMapping("/guest/refresh")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> guestRefresh(@RequestBody @Valid GuestRefreshReqDto reqDto);


}
