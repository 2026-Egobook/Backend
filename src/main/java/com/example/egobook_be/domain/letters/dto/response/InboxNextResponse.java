package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class InboxNextResponse {

    @Schema(description = "도착한 편지. 없으면 null", nullable = true)
    private LetterDto letter;

    @Schema(description = "현재 편지 이용 제한 여부")
    private boolean restricted;
    @Schema(description = "제재 사유; 제한이 없으면 null")
    private String reason;
    @Schema(description = "제재 종료 시각 (한국 시각); 제한이 없으면 null")
    private LocalDateTime restrictedUntil;

    public InboxNextResponse withRestriction(boolean restricted, String reason, LocalDateTime restrictedUntil) {
        return toBuilder().restricted(restricted).reason(reason)
                .restrictedUntil(restrictedUntil).build();
    }

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
        private String backgroundImageUrl;
    }

    public static InboxNextResponse empty() {
        return InboxNextResponse.builder()
                .letter(null)
                .build();
    }
}
