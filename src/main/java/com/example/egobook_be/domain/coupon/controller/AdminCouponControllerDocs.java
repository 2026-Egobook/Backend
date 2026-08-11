package com.example.egobook_be.domain.coupon.controller;

import com.example.egobook_be.domain.coupon.dto.*;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Coupon Controller", description = "[관리자] 쿠폰 관리 API")
public interface AdminCouponControllerDocs {

    @Operation(summary = "쿠폰 등록 API", description = """
            새 쿠폰 코드를 등록합니다.

            [**Request Body**]
            - code : 영문/숫자/하이픈 20자 이하. 대문자로 정규화되어 저장되며 중복 시 409를 반환합니다.
            - targetType : ALL(전체) / INDIVIDUAL(개인)
            - targetAccountCode : INDIVIDUAL일 때 필수, ALL일 때는 지정할 수 없습니다. 실존하는 계정인지 검증합니다.
            - rewards : 잉크(INK)/아이템(ITEM) 보상을 동시에, 각각 복수로 담을 수 있습니다.
            - rewards 배열 순서가 그대로 sortOrder로 저장되어 유저 팝업 노출 순서가 됩니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "쿠폰 등록에 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "보상 또는 대상 정보가 올바르지 않습니다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "계정 또는 아이템을 찾을 수 없습니다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복된 코드입니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    ResponseEntity<GlobalResponse<CouponAdminResDto>> createCoupon(@Valid @RequestBody CouponAdminCreateReqDto reqDto);

    @Operation(summary = "전체 쿠폰 조회 API", description = """
            등록된 전체 쿠폰 목록을 등록 최신순으로 조회합니다.

            [**응답 활용**]
            - targetType이 INDIVIDUAL인 행에만 알림 전송 버튼을 노출해주세요.
            - notified가 true면 '전송 완료' 비활성 버튼으로 노출해주세요.
            - rewards는 sortOrder 순으로 정렬되어 반환됩니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "쿠폰 목록 조회에 성공했습니다."),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    ResponseEntity<GlobalResponse<SliceResponse<CouponAdminResDto>>> getCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "쿠폰 단건 조회 API", description = """
            수정 팝업에 기존 값을 채우기 위해 쿠폰 1건을 조회합니다.

            [**참고**]
            - 응답 형태는 목록 조회의 content 원소와 동일합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "쿠폰 조회에 성공했습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰입니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{couponId}")
    ResponseEntity<GlobalResponse<CouponAdminResDto>> getCoupon(@PathVariable Long couponId);

    @Operation(summary = "쿠폰 수정 API", description = """
            등록된 쿠폰 정보를 수정합니다. Request Body는 등록과 동일합니다.

            [**주의사항**]
            - rewards는 부분 수정이 아니라 전체 교체입니다. '-' 버튼으로 제거한 항목은 배열에서 빼고 전달해주세요.
            - 보상이 새로 생성되므로 수정 후 sortOrder가 배열 순서대로 재부여됩니다.
            - 등록과 달리 만료 일시의 과거 날짜 제약은 없습니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "쿠폰 수정에 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "보상 또는 대상 정보가 올바르지 않습니다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "쿠폰, 계정 또는 아이템을 찾을 수 없습니다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복된 코드입니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{couponId}")
    ResponseEntity<GlobalResponse<CouponAdminResDto>> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponAdminUpdateReqDto reqDto
    );

    @Operation(summary = "쿠폰 삭제 API", description = """
            쿠폰과 보상을 함께 삭제합니다. 복구되지 않으므로 삭제 확인 팝업을 띄워주세요.

            [**주의사항**]
            - 이미 사용된 이력이 있는 쿠폰은 지급 내역 추적이 끊기므로 삭제할 수 없습니다. (409)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "쿠폰 삭제에 성공했습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰입니다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용된 쿠폰은 삭제할 수 없습니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{couponId}")
    ResponseEntity<GlobalResponse<Void>> deleteCoupon(@PathVariable Long couponId);

    @Operation(summary = "개인 쿠폰 알림 전송 API", description = """
            개인 대상 쿠폰의 코드가 담긴 알림을 해당 유저에게 전송합니다. (FCM 푸시 포함)

            [**주의사항**]
            - 전체 대상 쿠폰은 400을 반환합니다. 전체 공지는 레드닷 API를 사용해주세요.
            - 이미 전송한 쿠폰은 409를 반환하여 재전송을 막습니다.
            - 전송 성공 시 notified가 true가 되며, 버튼을 '전송 완료' 비활성 상태로 바꿔주세요.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 전송에 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "개인 대상이 아니거나 만료된 쿠폰입니다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "쿠폰 또는 대상 유저를 찾을 수 없습니다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 알림을 전송한 쿠폰입니다.", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{couponId}/notify")
    ResponseEntity<GlobalResponse<CouponAdminNotifyResDto>> sendCouponNotification(@PathVariable Long couponId);
}