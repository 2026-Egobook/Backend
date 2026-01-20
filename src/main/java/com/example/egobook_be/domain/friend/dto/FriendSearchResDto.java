package com.example.egobook_be.domain.friend.dto;

import lombok.Builder;

@Builder
public record FriendSearchResDto(
        Long userId,
        String nickname,
        Integer level,
        String profileImageUrl
) {}
