package com.example.egobook_be.domain.ads.mapper;

import com.example.egobook_be.domain.ads.entity.AdRewardHistory;
import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdsMapper {

    /** AdRewardHistory 엔티티를 생성하는 함수 */
    public AdRewardHistory toEntity(String transactionId, User user, Integer rewardAmount, AdRewardType rewardType, String adUnitId, WeeklyCounsel weeklyCounsel){
        return AdRewardHistory.builder()
                .transactionId(transactionId)
                .user(user)
                .rewardAmount(rewardAmount)
                .rewardType(rewardType)
                .adUnitId(adUnitId)
                .weeklyCounsel(weeklyCounsel)
                .build();
    }
}
