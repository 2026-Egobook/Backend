package com.example.egobook_be.domain.shop.dto;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminItemResDto(
        Long id,
        String path,
        ItemCategory category,
        String name,
        Integer price,
        String imageUrl,
        String status,
        LocalDateTime createdAt
) {}
