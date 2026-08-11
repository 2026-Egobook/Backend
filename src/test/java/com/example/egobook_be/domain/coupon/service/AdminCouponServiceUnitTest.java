package com.example.egobook_be.domain.coupon.service;

import com.example.egobook_be.domain.coupon.dto.CouponAdminCreateReqDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminNotifyResDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminResDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminRewardReqDto;
import com.example.egobook_be.domain.coupon.dto.CouponAdminUpdateReqDto;
import com.example.egobook_be.domain.coupon.entity.Coupon;
import com.example.egobook_be.domain.coupon.entity.CouponReward;
import com.example.egobook_be.domain.coupon.enums.CouponRewardType;
import com.example.egobook_be.domain.coupon.enums.CouponTargetType;
import com.example.egobook_be.domain.coupon.exception.CouponErrorCode;
import com.example.egobook_be.domain.coupon.mapper.CouponMapper;
import com.example.egobook_be.domain.coupon.repository.CouponRepository;
import com.example.egobook_be.domain.coupon.repository.UserCouponRepository;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminCouponServiceUnitTest {

    @InjectMocks private AdminCouponService adminCouponService;
    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private NotificationService notificationService;

    @Spy private CouponMapper couponMapper = new CouponMapper();

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(30);

    private Coupon buildCoupon(Long id, String code, CouponTargetType targetType,
                               String targetAccountCode, LocalDateTime expiresAt,
                               List<CouponReward> rewards) {
        return Coupon.builder()
                .id(id)
                .code(code)
                .targetType(targetType)
                .targetAccountCode(targetAccountCode)
                .expiresAt(expiresAt)
                .rewards(new ArrayList<>(rewards))
                .build();
    }

    private CouponReward inkReward(int amount, int sortOrder) {
        return CouponReward.builder()
                .rewardType(CouponRewardType.INK)
                .inkAmount(amount)
                .sortOrder(sortOrder)
                .build();
    }

    private CouponAdminRewardReqDto inkReq(int amount) {
        return new CouponAdminRewardReqDto(CouponRewardType.INK, amount, null);
    }

    private CouponAdminRewardReqDto itemReq(Long itemId) {
        return new CouponAdminRewardReqDto(CouponRewardType.ITEM, null, itemId);
    }

    private CouponAdminCreateReqDto createReq(String code, CouponTargetType targetType,
                                              String accountCode, List<CouponAdminRewardReqDto> rewards) {
        return new CouponAdminCreateReqDto(code, targetType, accountCode, rewards, FUTURE);
    }

    private CouponAdminUpdateReqDto updateReq(String code, CouponTargetType targetType,
                                              String accountCode, List<CouponAdminRewardReqDto> rewards,
                                              LocalDateTime expiresAt) {
        return new CouponAdminUpdateReqDto(code, targetType, accountCode, rewards, expiresAt);
    }

    /** save()가 인자로 받은 엔티티를 그대로 돌려주도록 설정한다. */
    private void givenSaveReturnsArgument() {
        given(couponRepository.save(any(Coupon.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("createCoupon() 성공 케이스")
    class CreateSuccessCases {

        @Test
        @DisplayName("[성공 1] 전체 대상 쿠폰 등록 - 보상 배열 순서가 sortOrder로 저장됨")
        void success_createAllTargetCoupon() {
            // Given
            CouponAdminCreateReqDto reqDto = createReq("EVENT01", CouponTargetType.ALL, null,
                    List.of(inkReq(100), inkReq(24)));

            given(couponRepository.existsByCode("EVENT01")).willReturn(false);
            givenSaveReturnsArgument();

            // When
            CouponAdminResDto result = adminCouponService.createCoupon(reqDto);

            // Then
            assertThat(result.code()).isEqualTo("EVENT01");
            assertThat(result.targetType()).isEqualTo(CouponTargetType.ALL);
            assertThat(result.targetAccountCode()).isNull();
            assertThat(result.rewards()).hasSize(2);
            assertThat(result.rewards().get(0).inkAmount()).isEqualTo(100);
            assertThat(result.rewards().get(1).inkAmount()).isEqualTo(24);
            assertThat(result.notified()).isFalse();
            verify(userRepository, never()).existsByAccountCode(anyString());
        }

        @Test
        @DisplayName("[성공 2] 개인 대상 쿠폰 등록 - 계정 고유 코드 검증 후 저장")
        void success_createIndividualCoupon() {
            // Given
            CouponAdminCreateReqDto reqDto = createReq("PRIVATE01", CouponTargetType.INDIVIDUAL, "USER01",
                    List.of(inkReq(12)));

            given(couponRepository.existsByCode("PRIVATE01")).willReturn(false);
            given(userRepository.existsByAccountCode("USER01")).willReturn(true);
            givenSaveReturnsArgument();

            // When
            CouponAdminResDto result = adminCouponService.createCoupon(reqDto);

            // Then
            assertThat(result.targetType()).isEqualTo(CouponTargetType.INDIVIDUAL);
            assertThat(result.targetAccountCode()).isEqualTo("USER01");
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("[성공 3] 혼합 보상 쿠폰 등록 - 아이템 존재 여부를 IN 절로 일괄 검증")
        void success_createMixedRewards() {
            // Given
            Long itemId = 10L;
            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);

            CouponAdminCreateReqDto reqDto = createReq("MIXED01", CouponTargetType.ALL, null,
                    List.of(inkReq(50), itemReq(itemId), inkReq(30)));

            given(couponRepository.existsByCode("MIXED01")).willReturn(false);
            given(itemRepository.findAllById(List.of(itemId))).willReturn(List.of(mockItem));
            givenSaveReturnsArgument();

            // When
            CouponAdminResDto result = adminCouponService.createCoupon(reqDto);

            // Then
            assertThat(result.rewards()).hasSize(3);
            assertThat(result.rewards().get(0).rewardType()).isEqualTo(CouponRewardType.INK);
            assertThat(result.rewards().get(1).rewardType()).isEqualTo(CouponRewardType.ITEM);
            assertThat(result.rewards().get(1).itemId()).isEqualTo(itemId);
            assertThat(result.rewards().get(2).inkAmount()).isEqualTo(30);
            verify(itemRepository, times(1)).findAllById(any());
        }
    }

    @Nested
    @DisplayName("updateCoupon() / deleteCoupon() 성공 케이스")
    class UpdateDeleteSuccessCases {

        @Test
        @DisplayName("[성공 4] 쿠폰 수정 - 보상 목록이 전달받은 값으로 전체 교체됨")
        void success_updateReplacesRewards() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "OLD01", CouponTargetType.ALL, null, FUTURE,
                    List.of(inkReward(100, 0), inkReward(24, 1)));

            given(couponRepository.findByIdWithRewards(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByCouponId(couponId)).willReturn(false);
            given(couponRepository.existsByCodeAndIdNot("NEW01", couponId)).willReturn(false);

            CouponAdminUpdateReqDto reqDto = updateReq("NEW01", CouponTargetType.ALL, null,
                    List.of(inkReq(500)), FUTURE);

            // When
            CouponAdminResDto result = adminCouponService.updateCoupon(couponId, reqDto);

            // Then
            assertThat(result.code()).isEqualTo("NEW01");
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).inkAmount()).isEqualTo(500);
            assertThat(coupon.getRewards()).hasSize(1);
        }

        @Test
        @DisplayName("[성공 5] 사용 이력 있는 쿠폰 - 만료일만 변경하면 수정 허용")
        void success_updateUsedCouponExpiresOnly() {
            // Given
            Long couponId = 1L;
            CouponReward originalReward = inkReward(100, 0);
            Coupon coupon = buildCoupon(couponId, "USED01", CouponTargetType.INDIVIDUAL, "USER01", FUTURE,
                    List.of(originalReward));

            given(couponRepository.findByIdWithRewards(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByCouponId(couponId)).willReturn(true);
            given(couponRepository.existsByCodeAndIdNot("USED01", couponId)).willReturn(false);
            given(userRepository.existsByAccountCode("USER01")).willReturn(true);

            LocalDateTime extended = FUTURE.plusDays(30);
            CouponAdminUpdateReqDto reqDto = updateReq("USED01", CouponTargetType.INDIVIDUAL, "USER01",
                    List.of(inkReq(100)), extended);

            // When
            CouponAdminResDto result = adminCouponService.updateCoupon(couponId, reqDto);

            // ========= Then =========
            assertThat(result.expiresAt()).isEqualTo(extended);
            // 사용 이력이 있으면 보상 인스턴스가 그대로 유지되어야 한다.
            assertThat(coupon.getRewards()).hasSize(1);
            assertThat(coupon.getRewards().get(0)).isSameAs(originalReward);
        }

        @Test
        @DisplayName("[성공 6] 사용 이력 없는 쿠폰 삭제")
        void success_deleteUnusedCoupon() {
            // Given
            Long couponId = 1L;
            given(couponRepository.existsById(couponId)).willReturn(true);
            given(userCouponRepository.existsByCouponId(couponId)).willReturn(false);

            // When
            adminCouponService.deleteCoupon(couponId);

            // Then
            verify(couponRepository, times(1)).deleteById(couponId);
        }
    }

    @Nested
    @DisplayName("sendCouponNotification() 성공 케이스")
    class NotifySuccessCases {

        @Test
        @DisplayName("[성공 7] 개인 쿠폰 알림 전송 - 조건부 UPDATE 선점 후 전송")
        void success_sendNotification() {
            // Given
            Long couponId = 1L;
            User mockUser = mock(User.class);
            Coupon coupon = buildCoupon(couponId, "PRIVATE01", CouponTargetType.INDIVIDUAL, "USER01", FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(couponRepository.markNotifiedIfNotYet(eq(couponId), any(LocalDateTime.class))).willReturn(1);
            given(userRepository.findByAccountCode("USER01")).willReturn(Optional.of(mockUser));
            given(notificationService.createCouponNotification(mockUser, couponId, "PRIVATE01"))
                    .willReturn(true);

            // When
            CouponAdminNotifyResDto result = adminCouponService.sendCouponNotification(couponId);

            // Then
            assertThat(result.notified()).isTrue();
            assertThat(result.notifiedAt()).isNotNull();
            assertThat(result.sentCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("createCoupon() 실패 케이스")
    class CreateFailCases {

        @Test
        @DisplayName("[실패 1] 중복된 쿠폰 코드")
        void fail_duplicatedCode() {
            // Given
            given(couponRepository.existsByCode("DUP01")).willReturn(true);
            CouponAdminCreateReqDto reqDto = createReq("DUP01", CouponTargetType.ALL, null, List.of(inkReq(100)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.DUPLICATED_CODE);
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 2] 개인 대상인데 계정 고유 코드 누락")
        void fail_accountCodeRequired() {
            // Given
            given(couponRepository.existsByCode("PRIVATE01")).willReturn(false);
            CouponAdminCreateReqDto reqDto = createReq("PRIVATE01", CouponTargetType.INDIVIDUAL, null,
                    List.of(inkReq(100)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.TARGET_ACCOUNT_CODE_REQUIRED);
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 3] 전체 대상인데 계정 고유 코드 지정")
        void fail_accountCodeNotAllowed() {
            // Given
            given(couponRepository.existsByCode("EVENT01")).willReturn(false);
            CouponAdminCreateReqDto reqDto = createReq("EVENT01", CouponTargetType.ALL, "USER01",
                    List.of(inkReq(100)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.TARGET_ACCOUNT_CODE_NOT_ALLOWED);
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 4] 존재하지 않는 계정 고유 코드")
        void fail_targetUserNotFound() {
            // Given
            given(couponRepository.existsByCode("PRIVATE01")).willReturn(false);
            given(userRepository.existsByAccountCode("NOBODY")).willReturn(false);
            CouponAdminCreateReqDto reqDto = createReq("PRIVATE01", CouponTargetType.INDIVIDUAL, "NOBODY",
                    List.of(inkReq(100)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.TARGET_USER_NOT_FOUND);
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 5] 존재하지 않는 보상 아이템")
        void fail_rewardItemNotFound() {
            // Given
            given(couponRepository.existsByCode("EVENT01")).willReturn(false);
            given(itemRepository.findAllById(List.of(999L))).willReturn(List.of());
            CouponAdminCreateReqDto reqDto = createReq("EVENT01", CouponTargetType.ALL, null,
                    List.of(itemReq(999L)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.REWARD_ITEM_NOT_FOUND);
            verify(couponRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패 6] 잉크 보상인데 수량 누락")
        void fail_invalidInkReward() {
            // Given
            given(couponRepository.existsByCode("EVENT01")).willReturn(false);
            CouponAdminCreateReqDto reqDto = createReq("EVENT01", CouponTargetType.ALL, null,
                    List.of(new CouponAdminRewardReqDto(CouponRewardType.INK, null, null)));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.createCoupon(reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.INVALID_REWARD);
            verify(couponRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCoupon() / deleteCoupon() 실패 케이스")
    class UpdateDeleteFailCases {

        @Test
        @DisplayName("[실패 7] 사용 이력 있는 쿠폰의 보상 변경 시도")
        void fail_updateUsedCouponRewards() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "USED01", CouponTargetType.ALL, null, FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findByIdWithRewards(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByCouponId(couponId)).willReturn(true);

            CouponAdminUpdateReqDto reqDto = updateReq("USED01", CouponTargetType.ALL, null,
                    List.of(inkReq(9999)), FUTURE);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.updateCoupon(couponId, reqDto));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.USED_COUPON_LIMITED_UPDATE);
            assertThat(coupon.getRewards().get(0).getInkAmount()).isEqualTo(100);
        }

        @Test
        @DisplayName("[실패 8] 사용 이력 있는 쿠폰 삭제 시도")
        void fail_deleteUsedCoupon() {
            // Given
            Long couponId = 1L;
            given(couponRepository.existsById(couponId)).willReturn(true);
            given(userCouponRepository.existsByCouponId(couponId)).willReturn(true);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.deleteCoupon(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_USED_CANNOT_DELETE);
            verify(couponRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("[실패 9] 존재하지 않는 쿠폰 삭제 시도")
        void fail_deleteCouponNotFound() {
            // Given
            Long couponId = 999L;
            given(couponRepository.existsById(couponId)).willReturn(false);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.deleteCoupon(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRepository, never()).existsByCouponId(anyLong());
        }
    }

    @Nested
    @DisplayName("sendCouponNotification() 실패 케이스")
    class NotifyFailCases {

        @Test
        @DisplayName("[실패 10] 전체 대상 쿠폰에 알림 전송 시도")
        void fail_notifyAllTargetCoupon() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "EVENT01", CouponTargetType.ALL, null, FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.sendCouponNotification(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.NOTIFY_TARGET_NOT_INDIVIDUAL);
            assertThat(coupon.isNotified()).isFalse();
            verify(notificationService, never()).createCouponNotification(any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("[실패 11] 이미 알림을 전송한 쿠폰 - 조건부 UPDATE가 0건이면 차단")
        void fail_alreadyNotified() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "PRIVATE01", CouponTargetType.INDIVIDUAL, "USER01", FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(couponRepository.markNotifiedIfNotYet(eq(couponId), any(LocalDateTime.class))).willReturn(0);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.sendCouponNotification(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_NOTIFIED);
            verify(notificationService, never()).createCouponNotification(any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("[실패 12] 만료된 쿠폰에 알림 전송 시도")
        void fail_notifyExpiredCoupon() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "EXPIRED01", CouponTargetType.INDIVIDUAL, "USER01",
                    LocalDateTime.now().minusDays(1), List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.sendCouponNotification(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.COUPON_EXPIRED);
            assertThat(coupon.isNotified()).isFalse();
        }

        @Test
        @DisplayName("[실패 13] 대상 유저가 알림을 꺼둔 경우 - 예외로 롤백되어 재전송 가능")
        void fail_receiverNotificationDisabled() {
            // Given
            Long couponId = 1L;
            User mockUser = mock(User.class);
            Coupon coupon = buildCoupon(couponId, "PRIVATE01", CouponTargetType.INDIVIDUAL, "USER01", FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(couponRepository.markNotifiedIfNotYet(eq(couponId), any(LocalDateTime.class))).willReturn(1);
            given(userRepository.findByAccountCode("USER01")).willReturn(Optional.of(mockUser));
            given(notificationService.createCouponNotification(mockUser, couponId, "PRIVATE01"))
                    .willReturn(false);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.sendCouponNotification(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.RECEIVER_NOTIFICATION_DISABLED);
        }

        @Test
        @DisplayName("[실패 14] 대상 유저를 찾을 수 없음")
        void fail_notifyTargetUserNotFound() {
            // Given
            Long couponId = 1L;
            Coupon coupon = buildCoupon(couponId, "PRIVATE01", CouponTargetType.INDIVIDUAL, "GONE01", FUTURE,
                    List.of(inkReward(100, 0)));

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(couponRepository.markNotifiedIfNotYet(eq(couponId), any(LocalDateTime.class))).willReturn(1);
            given(userRepository.findByAccountCode("GONE01")).willReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminCouponService.sendCouponNotification(couponId));

            assertThat(exception.getErrorCode()).isEqualTo(CouponErrorCode.TARGET_USER_NOT_FOUND);
            verify(notificationService, never()).createCouponNotification(any(), anyLong(), anyString());
        }
    }
}