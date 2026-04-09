package com.example.egobook_be.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ResendReqDto {

    @NotEmpty(message = "재발송 대상 ID 목록이 비어 있습니다.")
    private List<Long> failIds;
}