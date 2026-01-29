package com.example.egobook_be.domain.shop.dto;

import lombok.Builder;

/**
 * 한개의 아이템에 대한 정보를 담은 DTO
 * @param itemId Item PK
 * @param imageUrl Item의 S3 경로
 * @param price 아이템 가격
 * @param isPurchased 해당 아이템이 구매되었는지 여부
 * @param isEquipped 해당 아이템이 장착되었는지 여부
 */
@Builder
public record ItemInfoResDto(
        Long itemId,
        String imageUrl,
        Integer price,
        Boolean isPurchased,
        Boolean isEquipped
) {
}
