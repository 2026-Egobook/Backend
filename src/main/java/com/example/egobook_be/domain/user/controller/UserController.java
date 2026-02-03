package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.sevice.AuthService;
import com.example.egobook_be.domain.user.dto.UserNicknameResDto;
import com.example.egobook_be.domain.user.dto.UserNicknameUpdateReqDto;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.user.service.UserService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs{
    private final AuthService authService;
    private final UserService userService;

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
    public ResponseEntity<GlobalResponse<UserNicknameResDto>> updateNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid UserNicknameUpdateReqDto reqDto
    ){
        UserNicknameResDto resDto = userService.updateNickname(userId, reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("사용자 닉네임 업데이트 성공", resDto));
    }

    @Override
    public ResponseEntity<GlobalResponse<Void>> withdrawAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(null);
    }

}
