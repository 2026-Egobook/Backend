package com.example.egobook_be.domain.ads.controller;

import com.example.egobook_be.domain.ads.dto.AdAppendReqDto;
import com.example.egobook_be.domain.ads.dto.AdAppendResDto;
import com.example.egobook_be.domain.ads.service.AdsService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdsController implements AdsControllerDocs{
    private final AdsService adsService;

    @Override
    public ResponseEntity<GlobalResponse<AdAppendResDto>> appendAds(@ModelAttribute AdAppendReqDto reqDto){
        AdAppendResDto resDto = adsService.appendAds(reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("성공적으로 광고를 추가하였습니다.", resDto));
    }


}
