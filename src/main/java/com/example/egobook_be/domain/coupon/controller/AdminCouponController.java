package com.example.egobook_be.domain.coupon.controller;

import com.example.egobook_be.domain.coupon.dto.*;
import com.example.egobook_be.domain.coupon.service.AdminCouponService;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coupons")
public class AdminCouponController implements AdminCouponControllerDocs {

    private final AdminCouponService adminCouponService;

    /**
     * [쿠폰 등록]
     * POST /admin/coupons
     */
    @Override
    @PostMapping
    public ResponseEntity<GlobalResponse<CouponAdminResDto>> createCoupon(
            @Valid @RequestBody CouponAdminCreateReqDto reqDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "쿠폰 등록 성공", adminCouponService.createCoupon(reqDto)));
    }

    /**
     * [전체 쿠폰 조회]
     * GET /admin/coupons
     */
    @Override
    @GetMapping
    public ResponseEntity<GlobalResponse<SliceResponse<CouponAdminResDto>>> getCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("쿠폰 목록 조회 성공", adminCouponService.getCoupons(page, size))
        );
    }

    /**
     * [쿠폰 단건 조회]
     * GET /admin/coupons/{couponId}
     */
    @Override
    @GetMapping("/{couponId}")
    public ResponseEntity<GlobalResponse<CouponAdminResDto>> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(
                GlobalResponse.success("쿠폰 조회 성공", adminCouponService.getCoupon(couponId))
        );
    }

    /**
     * [쿠폰 수정]
     * PATCH /admin/coupons/{couponId}
     */
    @Override
    @PatchMapping("/{couponId}")
    public ResponseEntity<GlobalResponse<CouponAdminResDto>> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponAdminUpdateReqDto reqDto
    ) {
        return ResponseEntity.ok(
                GlobalResponse.success("쿠폰 수정 성공", adminCouponService.updateCoupon(couponId, reqDto))
        );
    }

    /**
     * [쿠폰 삭제]
     * DELETE /admin/coupons/{couponId}
     */
    @Override
    @DeleteMapping("/{couponId}")
    public ResponseEntity<GlobalResponse<Void>> deleteCoupon(@PathVariable Long couponId) {
        adminCouponService.deleteCoupon(couponId);
        return ResponseEntity.ok(GlobalResponse.success("쿠폰 삭제 성공", null));
    }

    /**
     * [개인 쿠폰 알림 전송]
     * POST /admin/coupons/{couponId}/notify
     */
    @Override
    @PostMapping("/{couponId}/notify")
    public ResponseEntity<GlobalResponse<CouponAdminNotifyResDto>> sendCouponNotification(@PathVariable Long couponId) {
        return ResponseEntity.ok(
                GlobalResponse.success("쿠폰 알림 전송 성공", adminCouponService.sendCouponNotification(couponId))
        );
    }
}