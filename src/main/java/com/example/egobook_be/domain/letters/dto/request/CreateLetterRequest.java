package com.example.egobook_be.domain.letters.dto.request;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateLetterRequest {

    @NotNull
    @Schema(description = "발송 모드", example = "RANDOM")
    private PlazaLetterMode mode;

    @Schema(description = "FRIEND 모드일 때 대상 유저 ID", example = "2", nullable = true)
    private Long toFriendId;

    @NotBlank
    @Size(max = 360)
    @Schema(description = "편지 내용(360자 이하)", example = "요즘 자꾸 불안해져서... 누가 한마디 해주면 좋겠어.")
    private String text;

    @Schema(description = "배경색(구독자 전용 가능), 기본은 하얀색", example = "WHITE", nullable = true)
    private String backgroundColor;
}
