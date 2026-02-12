package com.example.egobook_be.domain.ads.service;



import com.example.egobook_be.domain.ads.dto.TestAdRewardReqDto;
import com.example.egobook_be.domain.ads.dto.UserAdStatusResDto;

import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.domain.ads.enums.AdsErrorCode;
import com.example.egobook_be.domain.ads.mapper.AdsMapper;
import com.example.egobook_be.domain.ads.repository.AdRewardHistoryRepository;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.AdMobVerifier;
import com.example.egobook_be.global.util.InkLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdsService {
    private final AdMobVerifier adMobVerifier;
    private final AdRewardHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final InkLogRepository inkLogRepository;
    private final WeeklyCounselRepository weeklyCounselRepository;
    private final InkLogUtil inkLogUtil;
    private final AdsMapper adsMapper;

    private static final String KST_ZONE = "Asia/Seoul";
    private static final int DAILY_AD_LIMIT = 10;
    private static final int REWARD_PER_AD = 2; // 1회당 보상

    /**
     * AdMob SSV Callback 구현
     */
    @Transactional
    public void adMobCallbackInk(
            String queryString, String signature, String keyIdStr,
            String transactionId, String userIdStr, String weeklyCounselIdStr,
            String rewardType, String adUnitId
    ) {
        /*
          * 1. 보안 검증 - 주어진 SSV 서명을 key_id로 검증
          * - Swagger Test를 위해, 값이 "TEST_PASS"가 들어오면 검증을 패스하도록 설정한다.
         */
        if ("TEST_PASS".equals(signature)) {
            log.info("🚧 [Swagger Test] Pass verification.");
        } else {
            try {
                long keyId = Long.parseLong(keyIdStr);
                if (!adMobVerifier.verify(queryString, signature, keyId)) {
                    throw new SecurityException("AdMob Signature Failed");
                }
            } catch (NumberFormatException e) {
                throw new SecurityException("Invalid Key ID format");
            }
        }

        // 2. 멱등성(중복) 체크 - AdRewardHistory 엔티티에 인덱스 설정을 걸어두었으므로 빠르다
        if (historyRepository.existsByTransactionId(transactionId)) {
            return;
        }

        /*
         * 3. 보상이 어느 종류의 보상인지에 따라 수행되는 작업을 분류한다.
         * (1) rewardType이 AdRewardType.INK인 경우
         *  - 사용자에게 잉크 지급
         *  - 잉크 보상 Log 기록
         *  - AdRewardHistory에 해당 기록 추가
         * (2) rewardType이 AdRewardType.WEEK_COUNSEL인 경우
         *  - AdRewardHistory에 해당 기록 추가 (어떤 주간 AI 기록을 위한 광고를 본 것인지 기록)
         */
        AdRewardType type;
        try {
            type = AdRewardType.valueOf(rewardType);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[AdMob Callback] 정의되지 않은 보상 타입입니다: {}", rewardType);
            return;
        }
        switch (type) {
            case AdRewardType.INK -> {
                if (isDailyInkLimitReached(Long.parseLong(userIdStr))) {
                    log.warn("User {}는 일일 광고 시청 횟수를 초과했습니다. Transaction {}가 스킵되었습니다.", userIdStr, transactionId);
                    return; // 잉크 안 주고 종료
                }
                rewardInk(transactionId, userIdStr, adUnitId);
            }
            case AdRewardType.WEEK_COUNSEL -> rewardWeekCounsel(transactionId, userIdStr, weeklyCounselIdStr, adUnitId);
        }
    }

    /**
     * 광고 시청 시 Ink 보상을 주는 과정을 담은 함수
     * - 사용자가 하루 광고 횟수를 넘었는지 확인
     * - 사용자에에 잉크 지급
     * - 잉크 보상 Log 기록
     * - AdRewardHistory에 해당 기록 추가
     */
    private void rewardInk(String transactionId, String userIdStr, String adUnitId) {
        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        inkLogRepository.save(inkLogUtil.addInkAndGetInkLog(user, REWARD_PER_AD, InkLogType.WATCH_AD));
        // 이력 저장
        historyRepository.save(adsMapper.toEntity(transactionId, user, REWARD_PER_AD, AdRewardType.INK, adUnitId, null));
    }

    /**
     * 광고 시청 시 주간 AI 리포트를 시청하도록 해주는 과정을 담은 함수
     * - AdRewardHistory에 해당 기록(주간 AI 보고서)을 추가
     * - 해당 WeeklyCounsel의 isLocked 값 false로 업데이트
     */
    private void rewardWeekCounsel(String transactionId, String userIdStr, String weeklyCounselIdStr, String adUnitId){
        Long userId = Long.parseLong(userIdStr);

        /*
         * 1. 사용자 앱 -> AdMob -> Spring Boot 서버로 거쳐온 Custom Data를 검증한다.
         * - Custom Data에는 잠금 해제시킬 Weekly Counsel의 PK가 담겨있으므로, 해당 값이 없으면 해당 주간 보고서를 잠금해제시킬 수 없다.
         */
        if(weeklyCounselIdStr == null || weeklyCounselIdStr.isBlank()){
            log.error("[AdService::rewardWeekCounsel] Week Counsel ID is missing for transaction: {}", transactionId);
            return;
        }
        Long weeklyCounselId = Long.parseLong(weeklyCounselIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. 해당 보고서가 본인의 보고서인 경우에만 주간 보고서 조회
        WeeklyCounsel weeklyCounsel = weeklyCounselRepository.findByIdAndUser(weeklyCounselId, user)
                .orElseThrow(() -> new IllegalArgumentException("Weekly Counsel not found or has no authorization."));

        // 3. 해당 보고서 잠금 해제
        weeklyCounsel.updateLocked(false);

        // 4. 이력 저장
        historyRepository.save(adsMapper.toEntity(transactionId, user, 0, AdRewardType.WEEK_COUNSEL, adUnitId, weeklyCounsel));
        log.info("Unlocked Weekly Counsel {} via AdMob for User {}", weeklyCounselId, userId);
    }

    /**
     * [Helper] 잉크 광고 일일 제한 도달 여부 확인 (INK 타입만 카운트)
     */
    private boolean isDailyInkLimitReached(Long userId) {
        LocalDateTime nowKst = LocalDateTime.now(ZoneId.of(KST_ZONE));
        LocalDateTime startOfDay = nowKst.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = nowKst.toLocalDate().atTime(LocalTime.MAX);

        // Repository 메서드 이름 변경 및 파라미터 추가 (AdRewardType.INK)
        int currentCount = historyRepository.countDailyInkAds(userId, AdRewardType.INK, startOfDay, endOfDay);

        return currentCount >= DAILY_AD_LIMIT;
    }

    @Transactional
    public void grantTestAdReward(Long userId, TestAdRewardReqDto reqDto) {
        // 1. "TEST_" 접두사를 붙여서 랜덤 transactionId를 생성한다.
        String mockTransactionId = "TEST_" + UUID.randomUUID().toString();

        // 2. 멱등성(중복) 체크 - AdRewardHistory 엔티티에 인덱스 설정을 걸어두었으므로 빠르다
        if (historyRepository.existsByTransactionId(mockTransactionId)) {
            throw new CustomException(AdsErrorCode.TRANSACTION_ID_ALREADY_EXIST);
        }

        /*
         * 3. 보상이 어느 종류의 보상인지에 따라 수행되는 작업을 분류한다.
         * (1) rewardType이 AdRewardType.INK인 경우
         *  - 사용자에게 잉크 지급
         *  - 잉크 보상 Log 기록
         *  - AdRewardHistory에 해당 기록 추가
         * (2) rewardType이 AdRewardType.WEEK_COUNSEL인 경우
         *  - AdRewardHistory에 해당 기록 추가 (어떤 주간 AI 기록을 위한 광고를 본 것인지 기록)
         */
        AdRewardType type;
        try {
            type = AdRewardType.valueOf(reqDto.rewardType());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[AdMob Callback] 정의되지 않은 보상 타입입니다: {}", reqDto.rewardType());
            throw new CustomException(AdsErrorCode.UNDEFINED_AD_REWARD_TYPE);
        }
        switch (type) {
            case AdRewardType.INK -> {
                if (isDailyInkLimitReached(userId)) {
                    log.warn("User {}는 일일 광고 시청 횟수를 초과했습니다. Transaction {}가 스킵되었습니다.", userId, mockTransactionId);
                    throw new CustomException(AdsErrorCode.EXCEED_DAILY_ADS_NUM);
                }
                rewardInk(mockTransactionId, userId.toString(), reqDto.adUnitId());
            }
            case AdRewardType.WEEK_COUNSEL -> rewardWeekCounsel(mockTransactionId, userId.toString(), reqDto.targetId().toString(), reqDto.adUnitId());
        }
    }




    /**
     * 사용자의 오늘 광고 시청 관련 정보를 가져오는 함수
     * - 별도의 컬럼이나 테이블에 데이터를 저장해서 가져오지 않고, AdRewardHistory 테이블에 있는 데이터들로 필요한 정보들을 계산해서 데이터를 가져온다.
     */
    @Transactional(readOnly = true)
    public UserAdStatusResDto getUserAdStatus(Long userId) {
        // 1. 오늘 날짜 범위 구하기 (KST 00:00 ~ 23:59)
        LocalDateTime nowKst = LocalDateTime.now(ZoneId.of(KST_ZONE));
        LocalDateTime startOfDay = nowKst.toLocalDate().atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = nowKst.toLocalDate().atTime(LocalTime.MAX); // 23:59:59.999

        // 2. DB에서 오늘 잉크용 광고 시청 횟수 조회 (COUNT 쿼리, 인덱스 처리를 해두어서 빠름)
        int currentCount = historyRepository.countDailyInkAds(userId, AdRewardType.INK, startOfDay, endOfDay);

        // 3. 응답 생성
        boolean isAvailable = currentCount < DAILY_AD_LIMIT;

        return UserAdStatusResDto.builder()
                .currentViewCount(currentCount)
                .maxLimit(DAILY_AD_LIMIT)
                .isAvailable(isAvailable)
                .rewardPerAd(REWARD_PER_AD)
                .message(isAvailable ? "광고 보기" : "오늘은 더이상 광고를 볼 수 없어요")
                .build();
    }
}
