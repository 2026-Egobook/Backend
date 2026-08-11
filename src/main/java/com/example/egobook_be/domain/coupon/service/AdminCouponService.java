package com.example.egobook_be.domain.coupon.service;

import com.example.egobook_be.domain.coupon.dto.*;
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
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;
    private final CouponMapper couponMapper;

    /**
     * 쿠폰을 등록한다.
     * @param reqDto : 코드, 대상, 계정 고유 코드(개인 시), 보상 목록, 만료 일시
     */
    @Transactional
    public CouponAdminResDto createCoupon(CouponAdminCreateReqDto reqDto) {
        log.info("[AdminCouponService] createCoupon() - START | code: {}", reqDto.code());

        String code = reqDto.code().trim();
        if (couponRepository.existsByCode(code)) {
            throw new CustomException(CouponErrorCode.DUPLICATED_CODE);
        }

        String targetAccountCode = resolveTargetAccountCode(reqDto.targetType(), reqDto.targetAccountCode());

        Coupon coupon = Coupon.builder()
                .code(code)
                .targetType(reqDto.targetType())
                .targetAccountCode(targetAccountCode)
                .expiresAt(reqDto.expiresAt())
                .build();
        buildRewards(reqDto.rewards()).forEach(coupon::addReward);

        Coupon saved = couponRepository.save(coupon);
        CouponAdminResDto result = couponMapper.toCouponAdminResDto(saved);

        log.info("[AdminCouponService] createCoupon() - END | couponId: {}", saved.getId());
        return result;
    }

    /**
     * 전체 쿠폰 목록을 등록 최신순으로 조회한다. (만료된 쿠폰 포함)
     * - 만료 여부는 응답의 expiresAt으로 프론트에서 구분해 표시한다.
     * - 컬렉션 fetch join + 페이징의 메모리 페이징을 피하기 위해 ID 조회 후 일괄 fetch 방식으로 처리한다.
     * @param page : 페이지 번호 (1 ~ N, 프론트 기준)
     * @param size : 페이지 크기
     */
    @Transactional(readOnly = true)
    public SliceResponse<CouponAdminResDto> getCoupons(int page, int size) {
        log.info("[AdminCouponService] getCoupons() - START | page: {}", page);

        if (page < 1) {
            throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        }
        if (size < 1 || size > 100) {
            throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Slice<Long> idSlice = couponRepository.findCouponIds(pageable);

        List<CouponAdminResDto> content = Collections.emptyList();
        if (!idSlice.getContent().isEmpty()) {
            Map<Long, Coupon> couponMap = couponRepository.findAllWithRewardsByIdIn(idSlice.getContent())
                    .stream()
                    .collect(Collectors.toMap(Coupon::getId, Function.identity()));

            // 최신순 정렬은 ID 조회 결과를 기준으로 유지한다.
            content = idSlice.getContent().stream()
                    .map(couponMap::get)
                    .filter(Objects::nonNull)
                    .map(couponMapper::toCouponAdminResDto)
                    .toList();
        }

        SliceResponse<CouponAdminResDto> result = SliceResponse.<CouponAdminResDto>builder()
                .content(content)
                .page(idSlice.getNumber() + 1)
                .size(idSlice.getSize())
                .hasNext(idSlice.hasNext())
                .build();

        log.info("[AdminCouponService] getCoupons() - END | resultSize: {}", content.size());
        return result;
    }

    /** 쿠폰 단건을 조회한다. (수정 팝업 프리필용) */
    @Transactional(readOnly = true)
    public CouponAdminResDto getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithRewards(couponId)
                .orElseThrow(() -> new CustomException(CouponErrorCode.COUPON_NOT_FOUND));
        return couponMapper.toCouponAdminResDto(coupon);
    }

    /**
     * 쿠폰을 수정한다. 보상 목록은 전달받은 값으로 전체 교체된다.
     */
    @Transactional
    public CouponAdminResDto updateCoupon(Long couponId, CouponAdminUpdateReqDto reqDto) {
        log.info("[AdminCouponService] updateCoupon() - START | couponId: {}", couponId);

        Coupon coupon = couponRepository.findByIdWithRewards(couponId)
                .orElseThrow(() -> new CustomException(CouponErrorCode.COUPON_NOT_FOUND));

        String code = reqDto.code().trim();

        // 사용 이력이 있으면 지급 내역과 어긋나므로 코드/대상/보상 변경을 막고 만료일만 허용한다.
        boolean used = userCouponRepository.existsByCouponId(couponId);
        if (used) {
            boolean changed = !coupon.getCode().equals(code)
                    || coupon.getTargetType() != reqDto.targetType()
                    || !Objects.equals(coupon.getTargetAccountCode(), trimOrNull(reqDto.targetAccountCode()))
                    || isRewardChanged(coupon, reqDto.rewards());

            if (changed) {
                throw new CustomException(CouponErrorCode.USED_COUPON_LIMITED_UPDATE);
            }
        }

        if (couponRepository.existsByCodeAndIdNot(code, couponId)) {
            throw new CustomException(CouponErrorCode.DUPLICATED_CODE);
        }

        String targetAccountCode = resolveTargetAccountCode(reqDto.targetType(), reqDto.targetAccountCode());

        coupon.update(code, reqDto.targetType(), targetAccountCode, reqDto.expiresAt());

        // 사용 이력이 있으면 보상이 이미 동일함이 검증됐으므로, 불필요한 삭제/재생성으로 rewardId가 바뀌지 않도록 교체를 건너뛴다.
        if (!used) {
            coupon.replaceRewards(buildRewards(reqDto.rewards()));
        }

        CouponAdminResDto result = couponMapper.toCouponAdminResDto(coupon);

        log.info("[AdminCouponService] updateCoupon() - END | couponId: {}", couponId);
        return result;
    }

    /**
     * 쿠폰을 삭제한다.
     * - 사용 이력(user_coupon)이 있으면 지급 내역 추적이 끊기고 FK 제약에도 걸리므로 삭제를 막는다.
     */
    @Transactional
    public void deleteCoupon(Long couponId) {
        log.info("[AdminCouponService] deleteCoupon() - START | couponId: {}", couponId);

        if (!couponRepository.existsById(couponId)) {
            throw new CustomException(CouponErrorCode.COUPON_NOT_FOUND);
        }
        if (userCouponRepository.existsByCouponId(couponId)) {
            throw new CustomException(CouponErrorCode.COUPON_ALREADY_USED_CANNOT_DELETE);
        }
        couponRepository.deleteById(couponId);

        log.info("[AdminCouponService] deleteCoupon() - END | couponId: {}", couponId);
    }

    /**
     * 개인 대상 쿠폰의 코드가 담긴 알림을 해당 유저에게 전송한다.
     * - 전송 성공 시 notifiedAt을 기록하여 '전송 완료' 버튼 상태를 만든다.
     */
    @Transactional
    public CouponAdminNotifyResDto sendCouponNotification(Long couponId) {
        log.info("[AdminCouponService] sendCouponNotification() - START | couponId: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(CouponErrorCode.COUPON_NOT_FOUND));

        if (coupon.getTargetType() != CouponTargetType.INDIVIDUAL) {
            throw new CustomException(CouponErrorCode.NOTIFY_TARGET_NOT_INDIVIDUAL);
        }
        if (coupon.isNotified()) {
            throw new CustomException(CouponErrorCode.COUPON_ALREADY_NOTIFIED);
        }
        if (coupon.isExpired(LocalDateTime.now())) {
            throw new CustomException(CouponErrorCode.COUPON_EXPIRED);
        }

        User receiver = userRepository.findByAccountCode(coupon.getTargetAccountCode())
                .orElseThrow(() -> new CustomException(CouponErrorCode.TARGET_USER_NOT_FOUND));

        // 알림이 생성되지 않았는데 전송 완료로 표시하면 재전송이 영구히 막히므로 예외로 알린다.
        boolean created = notificationService.createCouponNotification(receiver, coupon.getId(), coupon.getCode());
        if (!created) {
            throw new CustomException(CouponErrorCode.RECEIVER_NOTIFICATION_DISABLED);
        }
        coupon.markNotified();

        log.info("[AdminCouponService] sendCouponNotification() - END | couponId: {}", couponId);
        return couponMapper.toCouponAdminNotifyResDto(coupon, 1);
    }

    // ===== private =====

    /**
     * 대상 구분에 따라 계정 고유 코드를 검증하고 저장할 값을 결정한다.
     * - 존재하지 않는 계정 코드가 저장되면 아무도 쓸 수 없는 쿠폰이 되므로 등록 시점에 검증한다.
     */
    private String resolveTargetAccountCode(CouponTargetType targetType, String accountCode) {
        boolean hasAccountCode = accountCode != null && !accountCode.isBlank();

        if (targetType == CouponTargetType.ALL) {
            if (hasAccountCode) {
                throw new CustomException(CouponErrorCode.TARGET_ACCOUNT_CODE_NOT_ALLOWED);
            }
            return null;
        }

        if (!hasAccountCode) {
            throw new CustomException(CouponErrorCode.TARGET_ACCOUNT_CODE_REQUIRED);
        }
        String trimmed = accountCode.trim();
        if (!userRepository.existsByAccountCode(trimmed)) {
            throw new CustomException(CouponErrorCode.TARGET_USER_NOT_FOUND);
        }
        return trimmed;
    }

    /**
     * 요청 DTO를 CouponReward 목록으로 변환한다.
     * - 배열 인덱스를 sortOrder로 저장하여 유저 팝업 노출 순서를 보장한다.
     * - 아이템 존재 여부는 IN 절로 한 번에 검증한다.
     */
    private List<CouponReward> buildRewards(List<CouponAdminRewardReqDto> reqDtos) {
        if (reqDtos == null || reqDtos.isEmpty()) {
            throw new CustomException(CouponErrorCode.REWARD_REQUIRED);
        }

        Set<Long> existingItemIds = findExistingItemIds(reqDtos);

        List<CouponReward> rewards = new ArrayList<>();
        for (int i = 0; i < reqDtos.size(); i++) {
            CouponAdminRewardReqDto reqDto = reqDtos.get(i);

            if (reqDto.rewardType() == CouponRewardType.INK) {
                if (reqDto.inkAmount() == null || reqDto.inkAmount() <= 0) {
                    throw new CustomException(CouponErrorCode.INVALID_REWARD);
                }
                rewards.add(CouponReward.ofInk(reqDto.inkAmount(), i));

            } else {
                if (reqDto.itemId() == null) {
                    throw new CustomException(CouponErrorCode.INVALID_REWARD);
                }
                if (!existingItemIds.contains(reqDto.itemId())) {
                    throw new CustomException(CouponErrorCode.REWARD_ITEM_NOT_FOUND);
                }
                rewards.add(CouponReward.ofItem(reqDto.itemId(), i));
            }
        }
        return rewards;
    }

    /** 요청에 포함된 아이템 ID 중 실제 존재하는 것들을 IN 절로 한 번에 조회한다. */
    private Set<Long> findExistingItemIds(List<CouponAdminRewardReqDto> reqDtos) {
        List<Long> itemIds = reqDtos.stream()
                .filter(r -> r.rewardType() == CouponRewardType.ITEM)
                .map(CouponAdminRewardReqDto::itemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (itemIds.isEmpty()) {
            return Collections.emptySet();
        }
        return itemRepository.findAllById(itemIds).stream()
                .map(Item::getId)
                .collect(Collectors.toSet());
    }

    private String trimOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 보상 구성(타입/수량/아이템/순서)이 달라졌는지 비교한다. */
    private boolean isRewardChanged(Coupon coupon, List<CouponAdminRewardReqDto> reqDtos) {
        List<CouponReward> current = coupon.getRewards().stream()
                .sorted(Comparator.comparing(CouponReward::getSortOrder))
                .toList();

        if (current.size() != reqDtos.size()) {
            return true;
        }
        for (int i = 0; i < current.size(); i++) {
            CouponReward now = current.get(i);
            CouponAdminRewardReqDto req = reqDtos.get(i);

            if (now.getRewardType() != req.rewardType()
                    || !Objects.equals(now.getInkAmount(), req.inkAmount())
                    || !Objects.equals(now.getItemId(), req.itemId())) {
                return true;
            }
        }
        return false;
    }
}