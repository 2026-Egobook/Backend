package com.example.egobook_be.domain.auth.dto.req;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleJoinReqDto(
        @Schema(description = "안드로이드가 구글로부터 받아온 ID Token", example = "ID Token")
        @NotBlank(message = "Google 회원가입을 위해서는 ID Token값이 필수입니다.")
        @JsonProperty("idToken")
        String idToken // 안드로이드가 구글에서 받아온 ID Token
){}
