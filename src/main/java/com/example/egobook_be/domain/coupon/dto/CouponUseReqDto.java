package com.example.egobook_be.domain.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponUseReqDto(
        @NotBlank(message = "쿠폰 코드를 입력해주세요")
        @Size(max = 20, message = "쿠폰 코드는 20자 이하여야 합니다")
        String code
) {}
