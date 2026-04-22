package com.example.egobook_be.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 관리자 로그인 성공 시 클라이언트에게 내려줄 토큰 정보
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminLoginResDto(
        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {
}
