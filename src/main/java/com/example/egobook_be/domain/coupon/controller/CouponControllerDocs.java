package com.example.egobook_be.domain.coupon.controller;

import com.example.egobook_be.domain.coupon.dto.CouponUseReqDto;
import com.example.egobook_be.domain.coupon.dto.CouponUseResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Coupon Controller", description = "[쿠폰] 관련 API")
@RequestMapping("/coupons")
public interface CouponControllerDocs {

    @Operation(summary = "쿠폰 사용 API", description = """
            쿠폰 코드를 입력하여 보상(잉크/아이템)을 지급받는 API입니다.

            [**Request Body**]
            - code : 쿠폰 코드 (공백 불가)

            [**기능**]
            - 쿠폰 코드의 유효성을 검증하고 보상을 지급합니다.
            - 보상 목록을 `sortOrder` 순서대로 반환하며, 이 순서대로 팝업을 노출하시면 됩니다.
            - 아이템을 이미 보유 중인 경우 중복 지급은 하지 않지만, 보상 목록에는 포함하여 팝업은 정상 노출되도록 해주세요.

            [**주의사항**]
            - 만료된 쿠폰, 이미 사용한 쿠폰, 본인 대상이 아닌 쿠폰, 20자를 넘긴 것들은 오류를 반환합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 사용에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = CouponUseResDto.class))),
            @ApiResponse(responseCode = "400", description = "만료된 쿠폰이거나 이미 사용한 쿠폰입니다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "해당 쿠폰을 사용할 수 없습니다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰 코드입니다.", content = @Content)
    })
    @PostMapping("/use")
    ResponseEntity<GlobalResponse<CouponUseResDto>> useCoupon(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid CouponUseReqDto reqDto
    );
}
