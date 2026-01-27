package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.sevice.AuthService;
import com.example.egobook_be.domain.user.dto.WithdrawReqDto;
import com.example.egobook_be.domain.user.dto.WithdrawResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs{
    private final AuthService authService;

    @Override
    public ResponseEntity<GlobalResponse<JwtTokenResDto>> linkGoogleAccount(
            @Parameter @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid GoogleJoinReqDto reqDto
    ){
        JwtTokenResDto tokenResDto = authService.linkGoogleAccount(userId, reqDto);
        return ResponseEntity
                .ok(GlobalResponse.success("Google 계정 연동이 완료되었습니다. 새로 발급된 Access, Refresh Token을 사용해주세요. (기존 Recover Token은 유효하지 않습니다)", tokenResDto));
    }

    @Override
    public ResponseEntity<GlobalResponse<WithdrawResDto>> withdrawUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid WithdrawReqDto reqDto
    ){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("회원 탈퇴가 정상적으로 처리되었습니다.",
                        WithdrawResDto.builder()
                                .deletedAt(LocalDateTime.now())
                                .graceDays(7)
                                .build()));
    }
}
