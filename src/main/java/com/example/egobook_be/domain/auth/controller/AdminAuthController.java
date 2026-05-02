package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.AdminAuthReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.AdminLoginResDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.sevice.AdminAuthService;
import com.example.egobook_be.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminAuthController implements AdminAuthControllerDocs {

    private final AdminAuthService adminAuthService;

    // [AI-GEN] 관리자 회원가입 엔드포인트 처리
    @Override
    public ResponseEntity<GlobalResponse<Void>> registerAdmin(@RequestBody @Valid AdminAuthReqDto reqDto) {
        adminAuthService.registerAdmin(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("관리자 회원가입이 완료되었습니다.", null));
    }

    // [AI-GEN] 관리자 로그인 엔드포인트 처리
    @Override
    public ResponseEntity<GlobalResponse<AdminLoginResDto>> loginAdmin(@RequestBody @Valid AdminAuthReqDto reqDto) {
        AdminLoginResDto resDto = adminAuthService.loginAdmin(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("로그인 성공입니다.", resDto));
    }

    /**
     * [관리자 Access Token 재발급]
     * POST /admin/auth/refresh
     */
    @Override
    public ResponseEntity<GlobalResponse<JwtTokenResDto>> refreshAccessToken(@RequestBody @Valid RefreshReqDto reqDto) {
        JwtTokenResDto jwtTokenResDto = adminAuthService.refreshAdminToken(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Access Token이 정상적으로 재발급되었습니다.", jwtTokenResDto));
    }

}
