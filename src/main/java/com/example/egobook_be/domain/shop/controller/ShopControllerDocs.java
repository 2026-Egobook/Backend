package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Shop Controller", description = "[상점] 관련 API")
@RequestMapping("/shop")
public interface ShopControllerDocs {
    @Operation(summary = "상점 아이템 조회", description = """
            특정 카테고리의 아이템들을 조회하는 API입니다.
            
            [**Query Parameter**]
            - category: **BACK** | **SKIN** | **DECOR_ONE** | **DECOR_TWO** | **BACKGROUND**
            - slice: 1 ~ n (slice값은 1부터 시작합니다.)
            
            [**기능**:]
            - 각 카테고리의 n slice의 데이터를 반환합니다.
            
            [**주의**]
             1. Slice 값을 0을 넣지 않도록 주의하세요.
             2. category 값은 필수입니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "해당 카테고리의 아이템 목록을 찾았습니다.",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 값을 보냈습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 카테고리의 아이템들을 찾지 못했습니다.",
                    content = @Content)
    })
    @GetMapping("/items")
    ResponseEntity<GlobalResponse<SliceResponse<ItemInfoResDto>>> getItemSlice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @Parameter(description = "아이템 카테고리", required = true)
            @RequestParam("category") ItemCategory category,

            @Parameter(description = "Slice 번호 (1 ~ N)")
            @RequestParam(value = "slice", defaultValue = "1") Integer slice,

            @Parameter(description = "Slice 크기")
            @RequestParam(value = "size", defaultValue = "6") Integer size
    );




}
