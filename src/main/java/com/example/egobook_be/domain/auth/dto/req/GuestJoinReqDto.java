package com.example.egobook_be.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Guest로 "회원가입(첫 로그인)" 요청을 할 때 사용되는 Request Dto
 */
@Builder
public record GuestJoinReqDto(
        @Schema(description = "기기 고유 식별자 (Device UUid)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "기기 고유 식별자(UID)는 필수입니다.")
        @JsonProperty("deviceUid")
        String deviceUid
) {
}
