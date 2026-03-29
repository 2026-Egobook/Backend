package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReplyResponse {

    private Long letterId;
    private PlazaLetterStatus status; // "REPLIED"
    private LocalDateTime repliedAt;
    private List<RewardDto> rewards;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    public static class RewardDto {
        private RewardKind kind;
        private int amount;
        private String toastMessage; // 필요하면 메시지 내려주기
    }

    public enum RewardKind {
        INK,
        EMPATHY
    }
}
