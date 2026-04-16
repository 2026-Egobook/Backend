package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.EquipItemReqDto;
import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.PurchaseItemReqDto;
import com.example.egobook_be.domain.shop.dto.ShopItemInfoResDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.service.ShopService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopController implements ShopControllerDocs{
    private final ShopService shopService;

    /**
     * [특정 카테고리 아이템 리스트 조회]
     * GET /shop/items?category=???&slice=1
     */
    @Override
    public ResponseEntity<GlobalResponse<SliceResponse<ShopItemInfoResDto>>> getItemSlice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @Parameter(description = "아이템 카테고리", required = true)
            @RequestParam("category") ItemCategory category,

            @Parameter(description = "Page 번호 (1 ~ N)")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "Page 크기")
            @RequestParam(value = "size", defaultValue = "6") Integer size
    ){
        SliceResponse<ShopItemInfoResDto> sliceResponse = shopService.getItemSlice(userId, category, page, size);
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

    /**
     * [아이템 장착/해제]
     * PATCH /shop/equip
     */
    @Override
    public ResponseEntity<GlobalResponse<ItemInfoResDto>> equipItem(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid EquipItemReqDto reqDto
    ){
        ItemInfoResDto resDto = shopService.equipItem(userId, reqDto);
        if(reqDto.isEquipped() == true){
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(GlobalResponse.success("아이템 장착 성공", resDto));
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("아이템 해제 성공", resDto));
    }

    @Override
    public ResponseEntity<GlobalResponse<List<ItemInfoResDto>>> getEquippedItems(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ){
        List<ItemInfoResDto> equippedItems = shopService.getEquippedItems(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("사용자가 장착하고 있는 아이템 리스트 조회 완료", equippedItems));
    }

}
