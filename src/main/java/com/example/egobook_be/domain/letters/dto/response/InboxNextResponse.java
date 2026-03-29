package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InboxNextResponse {

    @Schema(description = "도착한 편지. 없으면 null", nullable = true)
    private LetterDto letter;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class LetterDto {
        private Long letterId;
        private PlazaLetterStatus status;
        private PlazaLetterMode mode;
        private String fromLabel;
        private String content;
        private LocalDateTime arrivedAt;
        private LocalDateTime replyDeadlineAt;
        private String backgroundColor;
    }

    public static InboxNextResponse empty() {
        return InboxNextResponse.builder()
                .letter(null)
                .build();
    }
}
