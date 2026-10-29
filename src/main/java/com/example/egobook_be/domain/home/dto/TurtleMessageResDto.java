package com.example.egobook_be.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TurtleMessageResDto(
        @Schema(description = "고북이 말풍선에 표시할 문구 (등록된 문구가 없으면 null)",
                example = "기다리고 있었어요.", nullable = true)
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String message
) {
}
