package com.example.egobook_be.domain.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record FriendRequestCreateReqDto(
        @NotNull(message = "친구 신청 대상 userId는 필수입니다.")
        Long receiverId
) {
}
