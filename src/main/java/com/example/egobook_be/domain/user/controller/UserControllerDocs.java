package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.user.dto.FcmTokenReqDto;
import com.example.egobook_be.domain.user.dto.UserNicknameResDto;
import com.example.egobook_be.domain.user.dto.UserNicknameUpdateReqDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Controller", description = "사용자 관련 API")
@RequestMapping("/users")
public interface UserControllerDocs {
    @Operation(summary = "Guest -> Google 계정 연동 (데이터 이관)", description = """
            현재 로그인된 **Guest 사용자**의 계정을 **Google 계정**으로 전환합니다.
            
            - **기능**:
              1. 현재 Guest 유저의 정보(레벨, 잉크, 기록 등)를 그대로 유지합니다.
              2. 인증 수단을 Guest(Device UID)에서 Google(ID Token)로 교체합니다.
              3. **기존 Guest 인증 정보(DB, Redis)는 영구적으로 삭제**되며, 더 이상 해당 기기 ID로 로그인할 수 없습니다.
              4. 새로운 **Google용 Access/Refresh Token**을 발급하여 반환합니다.
            
            - **필수 조건**: 
              1. Header에 **Guest로 로그인한 Access Token**이 있어야 합니다.
              2. Body에 **유효한 Google ID Token**을 실어 보내야 합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계정 연동 성공 & 신규 토큰 발급 (RecoverToken is NULL)",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Google Token 유효성 실패 등)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패 (Guest 로그인이 되어있지 않음)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Guest 계정 정보를 찾을 수 없음",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 가입된 Google 계정입니다. (다른 계정에서 사용 중)",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth") // Swagger에서 자물쇠 버튼 활성화
    @PostMapping("/link/google")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> linkGoogleAccount(
            @Parameter @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid GoogleJoinReqDto reqDto
    );

    @Operation(summary = "사용자 닉네임 변경", description = """
            로그인된 사용자의 **닉네임(Nickname)**을 변경합니다.
            
            - **기능**:
              1. 입력된 닉네임의 형식(길이, 특수문자 등)을 검증합니다.
              2. 데이터베이스 내 **닉네임 중복 여부**를 확인합니다.
              3. 유효할 경우 영속성 컨텍스트를 업데이트하고 성공을 반환합니다.
            
            - **제약 사항**:
              1. 중복된 닉네임은 사용할 수 없습니다.
              2. 닉네임 정책(예: 2~8자 한글/영문/숫자)을 준수해야 합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 변경 성공",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임 형식",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 누락 또는 만료)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임 (중복)",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/nickname")
    ResponseEntity<GlobalResponse<UserNicknameResDto>> updateNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid UserNicknameUpdateReqDto reqDto
    );

    @Operation(summary = "사용자 회원 탈퇴", description = """
            현재 로그인된 사용자의 **계정을 삭제(탈퇴)** 처리합니다.
            
            - **기능**:
              1. 사용자의 모든 식별 정보를 삭제하거나 비식별화(Anonymization) 처리합니다.
              2. **Redis에 저장된 Refresh Token을 즉시 삭제**하여 모든 기기에서 로그아웃 시킵니다.
              3. 발급받은 AccessToken의 jti를 Redis의 BlackList에 해당 Token의 유효시간동안 등록하여, 추가적인 접근을 막습니다. 
              4. 해당 계정의 RefreshTokenBackup 테이블의 레코드를 삭제 및 사용자의 상태를 변화시킴으로써, Recovery Token으로 Refresh Token을 재발급받지 못하게 합니다.
              5. 더 이상 다른 사용자들은 해당 사용자에게 편지 작성, 친구 추가 등의 작업을 할 수 없습니다.   
              6. 탈퇴된 계정은 7일 뒤 완전히 정보가 삭제됩니다. 
            
            - **주의**: 
            탈퇴 처리된 계정은 복구가 불가능하며, 동일한 소셜 계정으로 재가입 시 신규 사용자로 처리됩니다.
            계정 복구를 원할 시, 고객센터로 연락해 복구해야합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공 (Resource Deleted)",
                    content = @Content(schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw")
    ResponseEntity<GlobalResponse<Void>> withdrawAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    );

    @Operation(summary = "유저 FCM 토큰 업데이트", description = """
            
            - 로그인한 사용자의 FCM 토큰을 등록하거나 업데이트합니다.
            - 사용자가 앱을 새로 설치하거나 재로그인, 또는 토큰이 만료되어 새로 발급된 경우 이 API를 호출하여 토큰을 최신 상태로 유지합니다.
            - 해당 토큰은 푸시 알람 발송 시 사용됩니다.
            """)
    @PatchMapping("/fcm-token")
    ResponseEntity<GlobalResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody FcmTokenReqDto dto
    );
}
