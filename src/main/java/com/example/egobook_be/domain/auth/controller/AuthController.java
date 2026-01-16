package com.example.egobook_be.domain.auth.controller;


import com.example.egobook_be.domain.auth.dto.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.JwtTokenDto;
import com.example.egobook_be.domain.auth.sevice.AuthService;
import com.example.egobook_be.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerDocs{
    private final AuthService authService;

    /**
     * [Guest 회원가입]
     * Post /auth/guest/join
     */
    @Override
    public ResponseEntity<GlobalResponse<JwtTokenDto>> guestJoin(@RequestBody @Valid GuestJoinReqDto reqDto){
        JwtTokenDto jwtTokenDto = authService.registerGuest(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("Guest Device Uid 서버에 등록 성공. Recover Token을 Device에 영구적으로 저장하세요.", jwtTokenDto));
    }
}
