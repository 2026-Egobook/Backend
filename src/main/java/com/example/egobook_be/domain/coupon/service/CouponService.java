package com.example.egobook_be.domain.coupon.service;

import com.example.egobook_be.domain.coupon.dto.CouponRewardResDto;
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
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final InkLogRepository inkLogRepository;
    private final InkLogUtil inkLogUtil;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    /**
     * 쿠폰 코드를 입력하여 보상을 지급하는 함수
     * - 코드 유효성(존재, 만료, 대상, 중복 사용) 검증 후 보상 순서대로 지급
     * - 잉크: 즉시 지급 및 InkLog 기록
     * - 아이템: 이미 보유 시 팝업은 노출하되 중복 지급하지 않음
     * @param userId  현재 로그인한 유저 ID
     * @param reqDto  쿠폰 코드
     * @return 지급된 보상 목록 (순서 보장)
     */
    @Transactional
    public CouponUseResDto useCoupon(Long userId, CouponUseReqDto reqDto) {
        log.info("[CouponService] useCoupon() - START | userId: {}, code: {}", userId, reqDto.code());

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 쿠폰 조회 (보상 목록 fetch join)
        Coupon coupon = couponRepository.findByCodeWithRewards(reqDto.code())
                .orElseThrow(() -> new CustomException(CouponErrorCode.COUPON_NOT_FOUND));

        // 3. 만료 여부 확인
        if (LocalDateTime.now().isAfter(coupon.getExpiresAt())) {
            throw new CustomException(CouponErrorCode.COUPON_EXPIRED);
        }

        // 4. 개인 쿠폰인 경우 대상 유저 확인
        if (coupon.getTargetType() == CouponTargetType.INDIVIDUAL) {
            if (!user.getAccountCode().equals(coupon.getTargetAccountCode())) {
                throw new CustomException(CouponErrorCode.COUPON_NOT_FOR_USER);
            }
        }

        // 5. 이미 사용한 쿠폰인지 확인
        if (userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())) {
            throw new CustomException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        // 6. 보상 지급
        List<InkLog> inkLogs = new ArrayList<>();
        List<CouponRewardResDto> rewardResults = new ArrayList<>();

        for (CouponReward reward : coupon.getRewards()) {
            if (reward.getRewardType() == CouponRewardType.INK) {
                inkLogUtil.addInkLogToList(inkLogs, user, reward.getInkAmount(), InkLogType.COUPON);
                rewardResults.add(CouponRewardResDto.builder()
                        .rewardType(CouponRewardType.INK)
                        .inkAmount(reward.getInkAmount())
                        .build());

            } else if (reward.getRewardType() == CouponRewardType.ITEM) {
                Item item = itemRepository.findById(reward.getItemId())
                        .orElseThrow(() -> new CustomException(CouponErrorCode.COUPON_NOT_FOUND));

                if (!userItemRepository.existsByUserIdAndItemId(userId, item.getId())) {
                    userItemRepository.save(UserItem.create(user, item));
                }

                rewardResults.add(CouponRewardResDto.builder()
                        .rewardType(CouponRewardType.ITEM)
                        .itemId(item.getId())
                        .itemName(item.getName())
                        .itemImageUrl(item.getFullUrl(cloudfrontDomain))
                        .build());
            }
        }

        inkLogRepository.saveAll(inkLogs);

        // 7. 사용 기록 저장
        userCouponRepository.save(UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build());

        log.info("[CouponService] useCoupon() - END | userId: {}, rewardCount: {}", userId, rewardResults.size());
        return CouponUseResDto.builder()
                .rewards(rewardResults)
                .build();
    }
}
