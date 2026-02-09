package com.example.egobook_be.domain.shop.mapper;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.ShopItemInfoResDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import org.springframework.stereotype.Component;

@Component
public class UserItemMapper {

    /**
     * UserItem Entity -> ItemInfoResDto로 변환하는 함수
     */
    public ItemInfoResDto toItemInfoResDto(UserItem userItem, Item item, String cloudfrontDomain) {
        return ItemInfoResDto.builder()
                .itemId(item.getId())
                .itemCategory(item.getCategory())
                .imageUrl(item.getFullUrl(cloudfrontDomain))
                .price(item.getPrice())
                .isPurchased(true)
                .isEquipped(userItem.getIsEquipped())
                .build();
    }
}
