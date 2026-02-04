package com.example.egobook_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserNicknameUpdateReqDto(
        @Schema(description = "변경할 새로운 닉네임", example = "에고북123")
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 8, message = "닉네임은 2자 이상 8자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^[a-zA-Z0-9가-힣]*$",
                message = "닉네임은 한글, 영문, 숫자만 사용할 수 있으며 공백이나 특수문자는 허용되지 않습니다."
        )
        String nickname
) {
}
