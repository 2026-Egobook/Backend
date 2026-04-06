package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.GuestRecertificationReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
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

    /**
     * [관리자 Recover Token으로 토큰 재발급]
     * POST /admin/auth/recertification
     */
    @Override
    public ResponseEntity<GlobalResponse<JwtTokenResDto>> recertificationAdminToken(@RequestBody @Valid GuestRecertificationReqDto reqDto) {
        JwtTokenResDto jwtTokenResDto = adminAuthService.recertificationAdminToken(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Access/Refresh/Recover Token이 정상적으로 재발급되었습니다.", jwtTokenResDto));
    }
}
