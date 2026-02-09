package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class DeferredInboxItemDto {
    private Long letterId;
    private PlazaLetterStatus status;   // DEFERRED
    private PlazaLetterMode mode;       // RANDOM / FRIEND

    private String fromLabel;           // FRIEND면 닉네임, RANDOM이면 "익명"
    private String backgroundColor;

    private String contentPreview;      // 30자 미리보기
    private OffsetDateTime arrivedAt;
    private OffsetDateTime replyDeadlineAt;
}

