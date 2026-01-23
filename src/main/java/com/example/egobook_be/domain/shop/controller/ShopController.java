package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.PurchaseItemReqDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.sevice.ShopService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShopController implements ShopControllerDocs{
    private final ShopService shopService;

    /**
     * [특정 카테고리 아이템 리스트 조회]
     * GET /shop/items?category=???&slice=1
     */
    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<ItemInfoResDto>>> getItemSlice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @Parameter(description = "아이템 카테고리", required = true)
            @RequestParam("category") ItemCategory category,

            @Parameter(description = "Slice 번호 (1 ~ N)")
            @RequestParam(value = "slice", defaultValue = "1") Integer slice,

            @Parameter(description = "Slice 크기")
            @RequestParam(value = "size", defaultValue = "6") Integer size
    ){
        SliceResponse<ItemInfoResDto> sliceResponse = shopService.getItemSlice(userId, category, slice, size);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(sliceResponse));
    }

    /**
     * [아이템 구매]
     * POST /shop/purchase
     */
    @Override
    public ResponseEntity<GlobalResponse<ItemInfoResDto>> purchaseItem(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid PurchaseItemReqDto reqDto
    ){
        ItemInfoResDto resDto = shopService.purchaseItem(userId, reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(resDto));
    }


}
