package com.example.egobook_be.domain.shop.dto;

import lombok.AllArgsConstructor;

/**
 * 사용자가 구매한 아이템의 정보를 담은 Dto
 * @param itemId
 * @param isEquipped
 */
public record UserItemStatusDto(
        Long itemId,
        boolean isEquipped
) {
    boolean equalsItemId(Long itemId) {return this.itemId.equals(itemId);}
}
