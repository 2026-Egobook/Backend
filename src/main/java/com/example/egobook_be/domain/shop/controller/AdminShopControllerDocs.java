package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.AdminItemListResDto;
import com.example.egobook_be.domain.shop.dto.AdminItemReqDto;
import com.example.egobook_be.domain.shop.dto.AdminItemResDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Tag(name = "Admin Shop Controller", description = "관리자 [상점] 관련 API")
public interface AdminShopControllerDocs {

    @Operation(summary = "아이템 추가", description = """
            
            상점에 들어갈 아이템을 추가합니다.
            
            - status 필드는 비워도 되고, 나머지 필드는 필수로 값을 넣어야합니다.
            - status 필드가 대소문자 상관없이 inactive: 비활성화 상태로 저장됩니다.
                비어있는 경우를 포함한 나머지: 활성화(active)로 저장
            - Send empty value 체크 X
            
            """)
    @PostMapping
    GlobalResponse<AdminItemResDto> createItem(@Valid @ModelAttribute AdminItemReqDto reqDto) throws IOException;

    @Operation(summary = "아이템 목록 확인", description = """
            
            아이템 목록을 조회합니다.
            
            - [카테고리] 전체를 보고싶으면 미입력 / 카테고리 필터링을 위해서는 반드시 대문자로 BACKGROUND, SKIN, BACK, DECOR_ONE, DECOR_TWO 중 하나를 입력해야합니다.
            - 페이지는 1 이상의 수를 입력해야합니다.
            """)
    @GetMapping
    GlobalResponse<AdminItemListResDto> getItemList(
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "아이템 수정", description = """
                       
            - 카테고리, 가격, 상태 필드 중 수정되는 필드가 아니더라도 기존 값을 채워서 요청을 보내야합니다.
            - 이미지 사진은 필수가 아닙니다. (넣지 않으면 기존 이미지 유지)
            - 상태 값은 ACTIVE , INACTIVE 둘 중 하나를 입력해야합니다.

            """)
    @PostMapping("/{itemId}")
    GlobalResponse<AdminItemResDto> updateItem(
            @PathVariable Long itemId,
            @Valid @ModelAttribute AdminItemReqDto reqDto) throws IOException;

    @Operation(summary = "아이템 삭제", description = """
            
            - 아이템을 삭제합니다.
            """)
    @DeleteMapping("/{itemId}")
    GlobalResponse<Void> deleteItem(@PathVariable Long itemId);

    @Operation(summary = "활성화 상태 변경", description = """
            
            - status에 ACTIVE, INACTIVE 둘 중 하나를 넣어 활성화 상태를 변경합니다.
            """)
    @PatchMapping("/{itemId}/status")
    GlobalResponse<Map<String,String>> changeStatus(@PathVariable Long itemId, @RequestParam String status);


}
