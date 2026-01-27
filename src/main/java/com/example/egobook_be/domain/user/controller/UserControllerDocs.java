package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.user.dto.WithdrawReqDto;
import com.example.egobook_be.domain.user.dto.WithdrawResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.security.CustomUserDetails;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    // [신규 추가] 회원 탈퇴 API Docs
    @Operation(summary = "회원 탈퇴", description = """
            현재 로그인된 사용자의 계정을 탈퇴 처리합니다.
            
            - **기능**:
              1. DB에서 사용자 정보를 Soft Delete 처리합니다.
              2. 사용자가 회원탈퇴한 후 7일 뒤에 사용자 데이터가 완전히 삭제됩니다.
            
            - **주의사항**:
              탈퇴 후에는 복구가 불가능할 수 있으며, 접근 토큰은 만료 처리됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "회원 탈퇴가 완료되었습니다."))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (Access 토큰 필요)",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw") // DELETE /users/withdraw
    ResponseEntity<GlobalResponse<WithdrawResDto>> withdrawUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid WithdrawReqDto reqDto
    );


}
