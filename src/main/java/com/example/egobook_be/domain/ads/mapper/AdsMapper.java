package com.example.egobook_be.domain.ads.mapper;

import com.example.egobook_be.domain.ads.dto.AdAppendReqDto;
import com.example.egobook_be.domain.ads.dto.AdAppendResDto;
import com.example.egobook_be.domain.ads.dto.CloudinaryUploadResult;
import com.example.egobook_be.domain.ads.entity.Ads;
import org.springframework.stereotype.Component;

@Component
public class AdsMapper {

    public Ads toEntity(AdAppendReqDto reqDto, CloudinaryUploadResult uploadResult){
        return Ads.builder()
                // 1. 미디어 & 식별 컬럼
                .advertiserName(reqDto.getAdvertiserName())
                .advertiserEmail(reqDto.getAdvertiserEmail())
                .cloudinaryPublicId(uploadResult.public_id())
                .title(reqDto.getTitle())
                .description(reqDto.getDescription())
                .ctaText(reqDto.getCtaText())
                .landingUrl(reqDto.getLandingUrl())
                // 2. 재생 규칙 컬럼
                .videoDurationSec(uploadResult.videoDurationSec())
                .rewardGrantSec(reqDto.getRewardGrantSec())
                // 3. 돈 & 예산 컬럼
                .totalBudget(reqDto.getTotalBudget())
                .remainingBudget(reqDto.getTotalBudget()) // 초기 잔액 = 총 예산
                .costPerView(reqDto.getCostPerView())
                .rewardInk(reqDto.getRewardInk())
                // 4. 스케줄링 및 상태 컬럼
                .startAt(reqDto.getStartAt())
                .endAt(reqDto.getEndAt())
                .build();
    }

    public AdAppendResDto toAdAppendResDto(Ads ads){
        return AdAppendResDto.builder()
                .adId(ads.getId())
                .title(ads.getTitle())
                .status(ads.getStatus())
                .build();
    }
}
