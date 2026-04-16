package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.AdminItemListResDto;
import com.example.egobook_be.domain.shop.dto.AdminItemReqDto;
import com.example.egobook_be.domain.shop.dto.AdminItemResDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.service.AdminShopService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/item")
public class AdminShopController implements AdminShopControllerDocs {

    private final AdminShopService adminShopService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GlobalResponse<AdminItemResDto> createItem(@Valid @ModelAttribute AdminItemReqDto reqDto) throws IOException {
        AdminItemResDto resDto=adminShopService.createItem(reqDto);
        return GlobalResponse.success(201, "아이템 등록 성공", resDto);
    }

    @GetMapping
    public GlobalResponse<AdminItemListResDto> getItemList(
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return GlobalResponse.success(adminShopService.getItemList(category, page, size));
    }

    // 아이템 수정
    @PostMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GlobalResponse<AdminItemResDto> updateItem(
            @PathVariable Long itemId,
            @Valid @ModelAttribute AdminItemReqDto reqDto) throws IOException {
        AdminItemResDto resDto=adminShopService.updateItem(itemId, reqDto);
        return GlobalResponse.success("아이템 수정 성공", resDto);
    }

    @DeleteMapping("/{itemId}")
    public GlobalResponse<Void> deleteItem(@PathVariable Long itemId) {
        adminShopService.deleteItem(itemId);
        return GlobalResponse.success("아이템 삭제 성공", null);
    }

    @PatchMapping("/{itemId}/status")
    public GlobalResponse<Map<String,String>> changeStatus(@PathVariable Long itemId, @RequestParam String status) {
        String updatedStatus = adminShopService.changeStatus(itemId, status);
        Map<String, String> res = Map.of("status", updatedStatus);

        return GlobalResponse.success("아이템 상태 변경 완료", res);
    }

}
