package com.example.egobook_be.domain.diary.dto;

import lombok.Builder;

@Builder
public record DiaryDeleteResDto (
    boolean deleted
) {}
