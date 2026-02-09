package com.example.egobook_be.domain.shop.controller;

import com.example.egobook_be.domain.shop.dto.EquipItemReqDto;
import com.example.egobook_be.domain.shop.dto.ItemInfoResDto;
import com.example.egobook_be.domain.shop.dto.PurchaseItemReqDto;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Shop Controller", description = "[상점] 관련 API")
@RequestMapping("/shop")
public interface ShopControllerDocs {
    @Operation(summary = "상점 아이템 조회", description = """
            특정 카테고리의 아이템들을 조회하는 API입니다.
            **상점용 아이템 이미지들의 url을 반환합니다**
            
            [**Query Parameter**]
            - category: **BACK** | **SKIN** | **DECOR_ONE** | **DECOR_TWO** | **BACKGROUND**
            - slice: 1 ~ n (page값은 1부터 시작합니다.)
            
            [**기능**:]
            - 각 카테고리의 n slice의 데이터를 반환합니다.
            
            [**주의**]
             1. Page 값을 0을 넣지 않도록 주의하세요.
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

            @Parameter(description = "Page 번호 (1 ~ N)")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "Page 크기")
            @RequestParam(value = "size", defaultValue = "6") Integer size
    );


    @Operation(summary = "아이템 구매 API", description = """
            특정 아이템을 구매하는 API입니다.
            **상점용 아이템 이미지들의 url을 반환합니다**
            
            [**Request Body**]
            - itemId : Item의 PK
            
            [**기능**]
            - 특정 아이템을 구매한 뒤 해당 아이템의 상태 정보만 다시 반환합니다.
            
            [**주의사항**]
            - 아이템 ID를 잘못 입력하면 안됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "아이템 구매에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = ItemInfoResDto.class))),
            @ApiResponse(responseCode = "404", description = "해당 아이템을 찾을 수 없습니다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "해당 아이템은 이미 구매되었습니다.", content = @Content),
            @ApiResponse(responseCode = "500", description = "아이템 구매 과정에서 알 수 없는 오류가 생겼습니다.", content = @Content),
    })
    @PostMapping("/purchase")
    ResponseEntity<GlobalResponse<ItemInfoResDto>> purchaseItem(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid PurchaseItemReqDto reqDto
    );

    @Operation(summary = "아이템 착용/해제 API", description = """
            보유 중인 아이템을 착용하는 API입니다.
            **사용자용 아이템 이미지들의 url을 반환합니다**
            
            [**Request Body**]
            - itemId : 착용할 Item의 PK
            
            [**기능**]
            - 해당 아이템을 장착(isEquipped = true)/해제(isEquipped = true) 상태로 변경합니다.
            - **동일 카테고리의 기존 장착 아이템은 자동으로 해제됩니다.**
            - 변경된 아이템의 정보를 반환합니다.
            
            [**주의사항**]
            - **구매하지 않은 아이템(UserItem 미존재)**은 착용/해제할 수 없습니다. (404 Error)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "아이템 착용/해제에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = ItemInfoResDto.class))),
            @ApiResponse(responseCode = "404", description = "보유하지 않은 아이템이거나 존재하지 않는 아이템입니다.", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터입니다.", content = @Content)
    })
    @PatchMapping("/equip") // 상태 변경이므로 PATCH 사용
    ResponseEntity<GlobalResponse<ItemInfoResDto>> equipItem(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid EquipItemReqDto reqDto
    );

    @Operation(summary = "착용 중인 아이템 목록 조회", description = """
            현재 사용자가 착용(Equipped)하고 있는 모든 아이템 정보를 조회하는 API입니다.
            **사용자용 아이템 이미지들의 url을 반환합니다**
            
            [**기능**]
            - 사용자의 인벤토리에서 `isEquipped` 상태가 `true`인 아이템들을 카테고리에 상관없이 모두 반환합니다.
            
            [**비즈니스 로직**]
            - `UserItem` 테이블에서 `userId`와 `isEquipped = true` 조건을 만족하는 데이터를 조회합니다.
            - 결과가 없을 경우 빈 리스트(`[]`)를 반환하며, 이는 404가 아닌 200 OK로 처리됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "착용 중인 아이템 목록을 성공적으로 조회했습니다.",
                    content = @Content(schema = @Schema(implementation = ItemInfoResDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자입니다.", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.", content = @Content)
    })
    @GetMapping("/items/equipped")
    ResponseEntity<GlobalResponse<List<ItemInfoResDto>>> getEquippedItems(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );
}
