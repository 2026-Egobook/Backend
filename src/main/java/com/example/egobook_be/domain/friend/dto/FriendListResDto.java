package com.example.egobook_be.domain.friend.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record FriendListResDto(
        int count,
        List<FriendResDto> friends
) {
}
