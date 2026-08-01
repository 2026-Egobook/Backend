package com.example.egobook_be.domain.coupon.service;

import com.example.egobook_be.domain.coupon.dto.CouponUseReqDto;
import com.example.egobook_be.domain.coupon.dto.CouponUseResDto;
import com.example.egobook_be.domain.coupon.entity.Coupon;
import com.example.egobook_be.domain.coupon.entity.CouponReward;
import com.example.egobook_be.domain.coupon.entity.UserCoupon;
import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import com.example.egobook_be.domain.coupon.enums.CouponTargetType;
import com.example.egobook_be.domain.coupon.exception.CouponErrorCode;
import com.example.egobook_be.domain.coupon.repository.CouponRepository;
import com.example.egobook_be.domain.coupon.repository.UserCouponRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceUnitTest {

    @InjectMocks private CouponService couponService;
    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private InkLogUtil inkLogUtil;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(couponService, "cloudfrontDomain", "https://test.cloudfront.net");
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────────

    private Coupon buildCoupon(CouponTargetType targetType, String targetAccountCode,
                               LocalDateTime expiresAt, List<CouponReward> rewards) {
        return Coupon.builder()
                .targetType(targetType)
                .targetAccountCode(targetAccountCode)
                .expiresAt(expiresAt)
                .rewards(rewards)
                .build();
    }

    private CouponReward inkReward(int amount) {
        return CouponReward.builder()
                .rewardType(CouponRewardType.INK)
                .inkAmount(amount)
                .sortOrder(1)
                .build();
    }

    private CouponReward itemReward(Long itemId) {
        return CouponReward.builder()
                .rewardType(CouponRewardType.ITEM)
                .itemId(itemId)
                .sortOrder(2)
                .build();
    }

    // ── 성공 케이스 ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("useCoupon() 성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("[성공 1] 잉크 보상 쿠폰 사용")
        void success_inkReward() {
            // ========= Given =========
            Long userId = 1L;
            User mockUser = mock(User.class);
            Coupon coupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().plusDays(7), List.of(inkReward(100)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("TEST-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(false);

            // ========= When =========
            CouponUseResDto result = couponService.useCoupon(userId, new CouponUseReqDto("TEST-CODE"));

            // ========= Then =========
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).rewardType()).isEqualTo(CouponRewardType.INK);
            assertThat(result.rewards().get(0).inkAmount()).isEqualTo(100);
            verify(inkLogUtil, times(1)).addInkLogToList(any(), eq(mockUser), eq(100), any());
            verify(inkLogRepository, times(1)).saveAll(any());
            verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        }

        @Test
        @DisplayName("[성공 2] 아이템 보상 쿠폰 사용 - 신규 아이템")
        void success_newItemReward() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 10L;
            User mockUser = mock(User.class);
            Item mockItem = mock(Item.class);

            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getName()).willReturn("거북이 스킨");
            given(mockItem.getFullUrl("https://test.cloudfront.net")).willReturn("https://test.cloudfront.net/items/skin.png");

            Coupon coupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().plusDays(7), List.of(itemReward(itemId)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("TEST-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(false);
            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(false);

            // ========= When =========
            CouponUseResDto result = couponService.useCoupon(userId, new CouponUseReqDto("TEST-CODE"));

            // ========= Then =========
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).rewardType()).isEqualTo(CouponRewardType.ITEM);
            assertThat(result.rewards().get(0).itemName()).isEqualTo("거북이 스킨");
            verify(userItemRepository, times(1)).save(any(UserItem.class));
        }

        @Test
        @DisplayName("[성공 3] 아이템 보상 쿠폰 - 이미 보유 시 팝업은 노출, DB 저장 생략")
        void success_duplicateItem_popupShownNotSaved() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 10L;
            User mockUser = mock(User.class);
            Item mockItem = mock(Item.class);

            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getName()).willReturn("거북이 스킨");
            given(mockItem.getFullUrl("https://test.cloudfront.net")).willReturn("https://test.cloudfront.net/items/skin.png");

            Coupon coupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().plusDays(7), List.of(itemReward(itemId)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("TEST-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(false);
            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(true); // 이미 보유

            // ========= When =========
            CouponUseResDto result = couponService.useCoupon(userId, new CouponUseReqDto("TEST-CODE"));

            // ========= Then =========
            assertThat(result.rewards()).hasSize(1); // 팝업 응답 포함
            assertThat(result.rewards().get(0).itemName()).isEqualTo("거북이 스킨");
            verify(userItemRepository, never()).save(any()); // DB 저장 생략
        }

        @Test
        @DisplayName("[성공 4] 혼합 보상 쿠폰 (잉크 + 아이템) - sortOrder 순서대로 반환")
        void success_mixedRewards() {
            // ========= Given =========
            Long userId = 1L;
            Long itemId = 10L;
            User mockUser = mock(User.class);
            Item mockItem = mock(Item.class);

            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getName()).willReturn("배경 아이템");
            given(mockItem.getFullUrl("https://test.cloudfront.net")).willReturn("https://test.cloudfront.net/items/bg.png");

            Coupon coupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().plusDays(7),
                    List.of(inkReward(50), itemReward(itemId)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("TEST-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(false);
            given(itemRepository.findById(itemId)).willReturn(Optional.of(mockItem));
            given(userItemRepository.existsByUserIdAndItemId(userId, itemId)).willReturn(false);

            // ========= When =========
            CouponUseResDto result = couponService.useCoupon(userId, new CouponUseReqDto("TEST-CODE"));

            // ========= Then =========
            assertThat(result.rewards()).hasSize(2);
            assertThat(result.rewards().get(0).rewardType()).isEqualTo(CouponRewardType.INK);
            assertThat(result.rewards().get(1).rewardType()).isEqualTo(CouponRewardType.ITEM);
            verify(inkLogUtil, times(1)).addInkLogToList(any(), eq(mockUser), eq(50), any());
            verify(userItemRepository, times(1)).save(any(UserItem.class));
        }

        @Test
        @DisplayName("[성공 5] INDIVIDUAL 타입 쿠폰 - 대상 유저 본인이 사용")
        void success_individualCoupon() {
            // ========= Given =========
            Long userId = 1L;
            User mockUser = mock(User.class);
            given(mockUser.getAccountCode()).willReturn("USER01");

            Coupon coupon = buildCoupon(CouponTargetType.INDIVIDUAL, "USER01",
                    LocalDateTime.now().plusDays(7), List.of(inkReward(200)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("PRIVATE-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(false);

            // ========= When =========
            CouponUseResDto result = couponService.useCoupon(userId, new CouponUseReqDto("PRIVATE-CODE"));

            // ========= Then =========
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).inkAmount()).isEqualTo(200);
        }
    }

    // ── 실패 케이스 ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("useCoupon() 실패 케이스")
    class FailCases {

        @Test
        @DisplayName("[실패 1] 유저를 찾을 수 없음")
        void fail_userNotFound() {
            // ========= Given =========
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> couponService.useCoupon(userId, new CouponUseReqDto("ANY")));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(couponRepository, never()).findByCodeWithRewards(any());
        }

        @Test
        @DisplayName("[실패 2] 존재하지 않는 쿠폰 코드")
        void fail_couponNotFound() {
            // ========= Given =========
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(mock(User.class)));
            given(couponRepository.findByCodeWithRewards("INVALID")).willReturn(Optional.empty());

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> couponService.useCoupon(userId, new CouponUseReqDto("INVALID")));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRepository, never()).existsByUserIdAndCouponId(any(), any());
        }

        @Test
        @DisplayName("[실패 3] 만료된 쿠폰")
        void fail_couponExpired() {
            // ========= Given =========
            Long userId = 1L;
            Coupon expiredCoupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().minusDays(1), List.of(inkReward(100)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(User.class)));
            given(couponRepository.findByCodeWithRewards("EXPIRED")).willReturn(Optional.of(expiredCoupon));

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> couponService.useCoupon(userId, new CouponUseReqDto("EXPIRED")));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_EXPIRED);
            verify(userCouponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 4] INDIVIDUAL 쿠폰 - 대상이 아닌 유저 사용 시도")
        void fail_couponNotForUser() {
            // ========= Given =========
            Long userId = 1L;
            User mockUser = mock(User.class);
            given(mockUser.getAccountCode()).willReturn("USER01");

            Coupon individualCoupon = buildCoupon(CouponTargetType.INDIVIDUAL, "OTHER99",
                    LocalDateTime.now().plusDays(7), List.of(inkReward(100)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(couponRepository.findByCodeWithRewards("PRIVATE")).willReturn(Optional.of(individualCoupon));

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> couponService.useCoupon(userId, new CouponUseReqDto("PRIVATE")));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_NOT_FOR_USER);
            verify(userCouponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 5] 이미 사용한 쿠폰")
        void fail_couponAlreadyUsed() {
            // ========= Given =========
            Long userId = 1L;
            Coupon coupon = buildCoupon(CouponTargetType.ALL, null,
                    LocalDateTime.now().plusDays(7), List.of(inkReward(100)));

            given(userRepository.findById(userId)).willReturn(Optional.of(mock(User.class)));
            given(couponRepository.findByCodeWithRewards("USED-CODE")).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())).willReturn(true);

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class,
                    () -> couponService.useCoupon(userId, new CouponUseReqDto("USED-CODE")));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_USED);
            verify(inkLogRepository, never()).saveAll(any());
            verify(userCouponRepository, never()).save(any());
        }
    }
}
