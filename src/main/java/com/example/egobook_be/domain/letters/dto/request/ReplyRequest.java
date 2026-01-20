package com.example.egobook_be.domain.letters.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ReplyRequest {

    @Schema(description = "답장 텍스트(350자 이하)", example = "너무 힘들었겠다.")
    @NotBlank(message = "text는 필수예요")
    @Size(max = 350, message = "text는 350자 이하여야 해요")
    private String text;
}
