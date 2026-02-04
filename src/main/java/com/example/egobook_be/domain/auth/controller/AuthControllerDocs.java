package com.example.egobook_be.domain.auth.controller;

import com.example.egobook_be.domain.auth.dto.req.*;
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

    @Operation(summary = "Access Token 재발급", description = """
            Access Token이 만료되었을 때, Refresh Token을 이용하여 토큰을 갱신합니다.
            [ 입력값 ]
            1. AccessToken: 만료되었거나 만료되지 않은, 기존에 사용하던 Access Token (Bearer 제외)
            - 기존에 사용하던 AccessToken을 Redis의 블랙리스트에 등록하기 위함입니다.
            2. RefreshToken: 만료되지 않은 Refresh Token (Bearer 제외)
            
            - **기능**: 유효한 Refresh Token을 검증하고, **새로운 Access Token**을 발급합니다.
            - **실패 시**: 401 Unauthorized 리턴 -> 클라이언트는 [Guest 복구(Recertification)] API를 호출해야 합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공 (Access 재발급. Refresh Token은 기존 토큰 그대로 반환합니다.)",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Token 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 유효하지 않음 (재로그인 필요)", content = @Content)
    })
    @PostMapping("/refresh")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> refreshAccessToken(@RequestBody @Valid RefreshReqDto reqDto);


    @Operation(summary = "Guest 계정 복구 (Refresh Token 만료 시)", description = """
            Refresh Token까지 만료되어(장기간 미접속 등) 로그인이 풀렸을 때 호출하는 API입니다.
            기기에 영구 저장된 **Recover Token**을 사용하여 새로운 Access, Refresh Token을 발급합니다.
            이때, **새로운 Recover Token 또한 발급합니다. 해당 Token을 다시 저장해야합니다.**
            
            - **기능**:
              1. 기기의 Recover Token과 서버의 Recover Token을 대조하여 검증합니다.
              2. 검증 성공 시, **새로운 Access Token과 Refresh Token**을 발급합니다.
              3. 새로운 Recover Token이 발급되어, 서버에 갱신됩니다. 새롭게 발급된 Recover Token을 다시 저장하세요.
            
            - **실패 시(401)**: Recover Token이 유효하지 않음 -> 해당 사용자의 계정을 삭제 대기 상태로 변환시킵니다. 더 이상 해당 계정에 접근할 수 없습니다. 고객센터에 문의하세요.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access/Refresh/Recover Token이 정상적으로 재발급되었습니다.",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (RecoverToken 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token 재발급 실패 (유효하지 않은 Recover Token 또는 기기 불일치)", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음 (탈퇴한 회원 등)", content = @Content)
    })
    @PostMapping("/guest/recertification")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> guestRecertification(@RequestBody @Valid GuestRecertificationReqDto reqDto);

    @Operation(summary = "Google 계정 복구 (Refresh Token 만료 시)", description = """
            Refresh Token까지 만료되어(장기간 미접속 등) 로그인이 풀렸을 때 호출하는 API입니다.
            FrontEnd가 Silent Google Login을 시도하여 보내준 IdToken으로 Access, Refresh Token을 발급합니다.
            이때, Recover Token은 발급되지 않습니다.
            
            - **기능**:
              1. FrontEnd가 Silent Google Login을 시도한 뒤 전달해준 ID Token을 검증합니다.
              2. 검증 성공 시, **새로운 Access Token과 Refresh Token**을 발급합니다.
            
            - **실패 시(401)**: Google ID Token이 유효하지 않음 -> 해당 사용자의 계정을 삭제 대기 상태로 변환시킵니다. 더 이상 해당 계정에 접근할 수 없습니다. 고객센터에 문의하세요.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access/Refresh Token이 정상적으로 재발급되었습니다.",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (RecoverToken 누락)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token 재발급 실패 (유효하지 않은 Google ID Token)", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음 (탈퇴한 회원 등)", content = @Content)
    })
    @PostMapping("/google/recertification")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> googleRecertification(@RequestBody @Valid GoogleRecertificationReqDto reqDto);


    @Operation(summary = "Google 회원가입 (소셜 계정 연동)", description = """
            안드로이드 앱에서 Google 로그인을 수행한 후, 발급받은 **ID Token**을 전송하여 회원가입을 수행합니다.
            
            - **기능**: 
              1. Google ID Token의 서명 및 Audience(Client ID)를 검증합니다.
              2. 검증된 정보로 신규 가입을 진행하고 **Access, Refresh Token**을 발급합니다.
            
            - **주의사항**: 
              1. **Google 로그인은 Recover Token을 발급하지 않습니다.** (Response의 recoverToken 값은 null입니다.)
                 - 이유: 계정 복구 및 신원 증명은 Google이 담당하기 때문입니다.
              2. 이미 가입된 Google 계정이라면 409 Conflict 에러가 발생합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공 및 토큰 발급 완료 (RecoverToken is NULL)",
                    content = @Content(schema = @Schema(implementation = JwtTokenResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (ID Token 누락 등)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 ID Token (위조되거나 만료된 토큰)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 가입된 계정 (중복 가입 시도)",
                    content = @Content)
    })
    @PostMapping("/google/join")
    ResponseEntity<GlobalResponse<JwtTokenResDto>> googleJoin(@RequestBody @Valid GoogleJoinReqDto reqDto);


}
