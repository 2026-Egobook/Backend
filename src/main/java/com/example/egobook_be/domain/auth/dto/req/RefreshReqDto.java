package com.example.egobook_be.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Guest로 로그인을 시도했을 때, Access Token이 만료된 경우 Token 재발급을 위해 호출 시 Dto
 * @param refreshToken : 클라이언트가 발급받은 Refresh Token
 */
@Builder
@Jacksonized
public record RefreshReqDto(
        @Schema(description = "만료되었거나 만료되지 않은, 기존에 사용하던 Access Token (Bearer 제외)", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Access Token은 필수입니다.")
        String accessToken,

        @Schema(description = "만료되지 않은 Refresh Token (Bearer 제외)", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}
