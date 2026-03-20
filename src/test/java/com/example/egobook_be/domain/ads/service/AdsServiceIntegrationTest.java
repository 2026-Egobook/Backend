package com.example.egobook_be.domain.ads.service;

import com.example.egobook_be.domain.ads.dto.TestAdRewardReqDto;
import com.example.egobook_be.domain.ads.dto.UserAdStatusResDto;
import com.example.egobook_be.domain.ads.entity.AdRewardHistory;
import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.domain.ads.enums.AdsErrorCode;
import com.example.egobook_be.domain.ads.repository.AdRewardHistoryRepository;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test") // application-test.yml (H2 DB 등 사용)
public class AdsServiceIntegrationTest {

    @Autowired private AdsService adsService;
    @Autowired private UserRepository userRepository;
    @Autowired private WeeklyCounselRepository weeklyCounselRepository;
    @Autowired private AdRewardHistoryRepository historyRepository;
    @Autowired private InkLogRepository inkLogRepository;

    private Long userId;
    private Long counselId;
    private final String AD_UNIT_ID = "ad_unit_abc";

    @BeforeEach
    public void setup() {
        // 1. 유저 생성 (초기 잉크 10)
        User user = User.builder()
                .accountCode("test")
                .email("test@example.com")
                .nickname("testNickname")
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .ink(10)
                .build();
        userRepository.save(user);
        this.userId = user.getId();

        // 2. 잠겨있는 주간 리포트 생성
        WeeklyCounsel counsel = WeeklyCounsel.builder()
                .user(user)
                .isLocked(true)
                .build();
        weeklyCounselRepository.save(counsel);
        this.counselId = counsel.getId();
    }

    @AfterEach
    void tearDown() {
        // FK 제약조건 오류를 방지하기 위해 자식 테이블부터 삭제
        historyRepository.deleteAllInBatch();
        inkLogRepository.deleteAllInBatch();
        weeklyCounselRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("adMobCallbackInk() 통합 테스트")
    class AdMobCallbackInkTest {

        @Test
        @DisplayName("[성공] INK 타입 - TEST_PASS 서명을 통해 잉크 보상이 정상 지급되고 DB에 기록된다")
        void successRewardInk() {
            // Given
            String transactionId = "tx_ink_001";

            // When
            adsService.adMobCallbackInk(
                    "query", "TEST_PASS", "123", transactionId,
                    userId.toString(), null, "INK", AD_UNIT_ID
            );

            // Then
            User updatedUser = userRepository.findById(userId).orElseThrow();
            assertThat(updatedUser.getInk()).isEqualTo(12); // 기존 10 + 보상 2 = 12

            long inkLogCount = inkLogRepository.countByUser(updatedUser);
            assertThat(inkLogCount).isEqualTo(1); // 잉크 로그 1개 생성

            boolean historyExists = historyRepository.existsByTransactionId(transactionId);
            assertThat(historyExists).isTrue(); // 히스토리 정상 기록
        }

        @Test
        @DisplayName("[성공] WEEK_COUNSEL 타입 - TEST_PASS 서명을 통해 주간 리포트 잠금이 해제된다")
        void successRewardWeekCounsel() {
            // Given
            String transactionId = "tx_counsel_001";

            // When
            adsService.adMobCallbackInk(
                    "query", "TEST_PASS", "123", transactionId,
                    userId.toString(), counselId.toString(), "WEEK_COUNSEL", AD_UNIT_ID
            );

            // Then
            WeeklyCounsel updatedCounsel = weeklyCounselRepository.findById(counselId).orElseThrow();
            assertThat(updatedCounsel.isLocked()).isFalse(); // 잠금 해제 확인

            boolean historyExists = historyRepository.existsByTransactionId(transactionId);
            assertThat(historyExists).isTrue(); // 히스토리 정상 기록
        }
    }

    @Nested
    @DisplayName("grantTestAdReward() 및 일일 한도 통합 테스트")
    class DailyLimitIntegrationTest {

        @Test
        @DisplayName("[성공 -> 실패] 광고 10번 시청 후 11번째 요청 시 일일 한도 초과 예외 발생")
        void failExceedDailyLimit() {
            // Given
            TestAdRewardReqDto reqDto = new TestAdRewardReqDto(AdRewardType.INK.name(), 1L, AD_UNIT_ID);

            // When & Then
            // 1. 정상적으로 10번의 광고 보상 요청 (DB에 오늘 날짜의 History가 10개 쌓임)
            for (int i = 0; i < 10; i++) {
                adsService.grantTestAdReward(userId, reqDto);
            }

            // DB 검증: 잉크가 10번 * 2 = 20 증가하여 총 30이 되었는지 확인
            User updatedUser = userRepository.findById(userId).orElseThrow();
            assertThat(updatedUser.getInk()).isEqualTo(30);

            // 2. 11번째 요청 시 일일 한도 초과 CustomException 발생 확인
            assertThatThrownBy(() -> adsService.grantTestAdReward(userId, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(AdsErrorCode.EXCEED_DAILY_ADS_NUM.getMessage());
        }
    }

    @Nested
    @DisplayName("getUserAdStatus() 통합 테스트")
    class GetUserAdStatusTest {

        @Test
        @DisplayName("[성공] 현재 광고 시청 횟수와 상태를 정확히 반환한다")
        void successGetUserAdStatus() {
            // Given
            TestAdRewardReqDto reqDto = new TestAdRewardReqDto(AdRewardType.INK.name(), 1L, AD_UNIT_ID);

            // 광고 3번 시청 처리
            adsService.grantTestAdReward(userId, reqDto);
            adsService.grantTestAdReward(userId, reqDto);
            adsService.grantTestAdReward(userId, reqDto);

            // When
            UserAdStatusResDto status = adsService.getUserAdStatus(userId);

            // Then
            assertThat(status.currentViewCount()).isEqualTo(3);
            assertThat(status.isAvailable()).isTrue();
            assertThat(status.message()).isEqualTo("광고 보기");
        }
    }
}