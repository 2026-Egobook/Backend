package com.example.egobook_be.domain.shop.dto;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record AdminItemReqDto(
        @NotNull(message = "필수 입력 사항입니다.")
        ItemCategory category,
        @NotNull(message = "필수 입력 사항입니다.")
        Integer price,
        MultipartFile file,
        String status
) {}