package com.example.egobook_be.domain.friend.dto;

import lombok.Builder;

@Builder
public record FriendResDto(
        Long friendId,
        String nickname
) {
}
