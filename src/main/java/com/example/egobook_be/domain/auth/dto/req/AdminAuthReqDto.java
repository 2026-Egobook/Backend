package com.example.egobook_be.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 회원가입 & 로그인 요청 시 사용되는 DTO
 */
public record AdminAuthReqDto(
        @Schema(description = "관리자 아이디", example = "admin123")
        @NotBlank(message = "관리자 아이디는 필수입니다.")
        String adminId,

        @Schema(description = "관리자 비밀번호", example = "securePassword123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
