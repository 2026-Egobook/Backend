package com.example.egobook_be.domain.coupon.controller;

import com.example.egobook_be.domain.coupon.dto.CouponUseReqDto;
import com.example.egobook_be.domain.coupon.dto.CouponUseResDto;
import com.example.egobook_be.domain.coupon.service.CouponService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponControllerDocs {

    private final CouponService couponService;

    /**
     * [쿠폰 사용]
     * POST /coupons/use
     */
    @Override
    public ResponseEntity<GlobalResponse<CouponUseResDto>> useCoupon(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @RequestBody @Valid CouponUseReqDto reqDto
    ) {
        CouponUseResDto resDto = couponService.useCoupon(userId, reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("쿠폰 사용 완료", resDto));
    }
}
