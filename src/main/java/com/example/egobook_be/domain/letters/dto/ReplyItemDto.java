package com.example.egobook_be.domain.letters.dto;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ReplyItemDto {
    private Long replyId;
    private Long letterId;
    private Long threadId;

    private PlazaLetterMode mode;
    private String backgroundColor;

    private String replyText;
    private OffsetDateTime createdAt;
}