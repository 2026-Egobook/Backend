package com.example.egobook_be.domain.shop.mapper;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.ShopItemInfoResDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    /**
     * Item -> ItemInfoResDto로 변환하는 함수
     * @param item
     * @return
     */
    public ItemInfoResDto toItemInfoResDto(Item item, String cloudfrontDomain, Boolean isPurchased, Boolean isEquipped){
        return ItemInfoResDto.builder()
                .itemId(item.getId())
                .imageUrl(item.getFullUrl(cloudfrontDomain))
                .price(item.getPrice())
                .isPurchased(isPurchased)
                .isEquipped(isEquipped)
                .build();
    }


    /** UserItem Entity -> ItemInfoResDto로 변환하는 함수 */
    public ShopItemInfoResDto toShopItemInfoResDto(Item item, String shopCloudfrontDomain, String myCloudfrontDomain, Boolean isPurchased, Boolean isEquipped){
        return ShopItemInfoResDto.builder()
                .itemId(item.getId())
                .shopImageUrl(item.getFullUrl(shopCloudfrontDomain))
                .myImageUrl(item.getFullUrl(myCloudfrontDomain))
                .price(item.getPrice())
                .isPurchased(isPurchased)
                .isEquipped(isEquipped)
                .build();
    }
}
