package com.example.egobook_be.domain.friend.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FriendRequestListResDto(
        Long requestId,
        Long userId,
        String nickname,
        LocalDateTime requestedAt
) {
}
