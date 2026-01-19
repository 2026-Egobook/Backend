package com.example.egobook_be.domain.letters.dto;

import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReplyResponse {

    private Long letterId;
    private PlazaLetterStatus status; // "REPLIED"
    private OffsetDateTime repliedAt;
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
        SINCERITY
    }
}
