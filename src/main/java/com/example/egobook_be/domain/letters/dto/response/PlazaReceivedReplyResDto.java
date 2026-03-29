package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlazaReceivedReplyResDto {

    private Long letterId;
    private Long replyId;
    private Long threadId;

    private String replyText;
    private LocalDateTime repliedAt;

    private boolean aiGenerated;
    private boolean reported;

    // 편지 정보
    private PlazaLetterMode mode;
    private String fromLabel;
    private String backgroundColor;
}
