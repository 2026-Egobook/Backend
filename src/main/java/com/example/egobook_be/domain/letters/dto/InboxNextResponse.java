package com.example.egobook_be.domain.letters.dto;

import com.example.egobook_be.domain.letters.domain.PlazaLetterMode;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

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
        private OffsetDateTime arrivedAt;
        private OffsetDateTime replyDeadlineAt;
    }

    public static InboxNextResponse empty() {
        return InboxNextResponse.builder()
                .letter(null)
                .build();
    }
}
