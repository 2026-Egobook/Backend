package com.example.egobook_be.domain.auth.controller;


import com.example.egobook_be.domain.auth.dto.req.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.req.GuestRefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.sevice.AuthService;
import com.example.egobook_be.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs{
    private final AuthService authService;

    /**
     * [Guest 회원가입]
     * Post /auth/guest/join
     */
    @Override
    public ResponseEntity<GlobalResponse<JwtTokenResDto>> guestJoin(@RequestBody @Valid GuestJoinReqDto reqDto){
        JwtTokenResDto jwtTokenResDto = authService.registerGuest(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Guest Device Uid 서버에 등록 성공. Recover Token을 Device에 영구적으로 저장하세요.", jwtTokenResDto));
    }

    /**
     * [Guest Access 토큰 재발급]
     * Post /auth/guest/refresh
     */
    @Override
    public ResponseEntity<GlobalResponse<JwtTokenResDto>> guestRefresh(@RequestBody @Valid GuestRefreshReqDto reqDto) {
        JwtTokenResDto jwtTokenResDto = authService.refreshGuestToken(reqDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Access Token이 정상적으로 갱신되었습니다.", jwtTokenResDto));
    }



}
