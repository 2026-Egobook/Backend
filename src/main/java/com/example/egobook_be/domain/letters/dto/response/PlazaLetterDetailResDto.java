package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class PlazaLetterDetailResDto {


    private Long letterId;
    private Long threadId;

    private PlazaLetterStatus status;
    private PlazaLetterMode mode;

    private String content;
    private String backgroundColor;

    private OffsetDateTime createdAt;
    private OffsetDateTime arrivedAt;

    // ===== 답장 =====
    private ReplyDto reply;   // 답장 없으면 null

    @Getter
    @Builder
    public static class ReplyDto {
        private Long replyId;
        private String text;

        private boolean aiGenerated;
        private boolean reported;

        private OffsetDateTime createdAt;
    }
}
