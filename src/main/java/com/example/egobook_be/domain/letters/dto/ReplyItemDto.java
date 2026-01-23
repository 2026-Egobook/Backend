package com.example.egobook_be.domain.letters.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ReplyItemDto {
    private Long replyId;
    private Long letterId;
    private String replyText;
    private OffsetDateTime createdAt;
}