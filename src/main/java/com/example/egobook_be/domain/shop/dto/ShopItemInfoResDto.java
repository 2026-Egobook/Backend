package com.example.egobook_be.domain.shop.dto;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import lombok.Builder;

@Builder
public record ShopItemInfoResDto(
        Long itemId,
        ItemCategory itemCategory,
        String shopImageUrl,
        String myImageUrl,
        Integer price,
        Boolean isPurchased,
        Boolean isEquipped
) {
}
