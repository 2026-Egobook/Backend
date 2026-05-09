package com.example.egobook_be.domain.question.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FriendAnswerResDto(
        Long answerId,
        Long userId,
        String nickname,
        Integer level,
        String content,
        LocalDateTime createdAt
) {
}

