package com.example.egobook_be.domain.auth.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record GuestRecertificationReqDto(
        @Schema(description = "Refresh Token을 재발급하기 위한 Device Uid", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "Device Uid는 필수입니다.")
        @JsonProperty("deviceUid")
        String deviceUid,

        @Schema(description = "Refresh Token을 재발급하기 위한 Recover Token", example = "eaifiiehig-afe...")
        @NotBlank(message = "Recover Token은 필수입니다.")
        @JsonProperty("recoverToken")
        String recoverToken
) {}
