package com.example.egobook_be.domain.ads.service;

import com.example.egobook_be.domain.ads.dto.TestAdRewardReqDto;
import com.example.egobook_be.domain.ads.dto.UserAdStatusResDto;
import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.domain.ads.enums.AdsErrorCode;
import com.example.egobook_be.domain.ads.mapper.AdsMapper;
import com.example.egobook_be.domain.ads.repository.AdRewardHistoryRepository;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.AdMobVerifier;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given; // BDDMockito 임포트 추가
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdsServiceUnitTest {

    @InjectMocks
    private AdsService adsService;

    @Mock private AdMobVerifier adMobVerifier;
    @Mock private AdRewardHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private InkLogRepository inkLogRepository;
    @Mock private WeeklyCounselRepository weeklyCounselRepository;
    @Mock private InkLogUtil inkLogUtil;
    @Mock private AdsMapper adsMapper;

    private User dummyUser;
    private WeeklyCounsel dummyCounsel;
    private final Long USER_ID = 1L;
    private final Long COUNSEL_ID = 100L;
    private final String TX_ID = "tx_12345";
    private final String AD_UNIT_ID = "ad_unit_abc";

    @BeforeEach
    void setUp() {
        dummyUser = User.builder().ink(10).build();
        ReflectionTestUtils.setField(dummyUser, "id", USER_ID);

        dummyCounsel = WeeklyCounsel.builder().user(dummyUser).isLocked(true).build();
        ReflectionTestUtils.setField(dummyCounsel, "id", COUNSEL_ID);
    }

    @Nested
    @DisplayName("adMobCallbackInk() 테스트")
    class AdMobCallbackInkTest {

        @Test
        @DisplayName("[성공] INK 보상 - 정상 서명 및 일일 한도 미달 시 잉크 지급")
        void successInkReward() {
            // ======== Given ========
            given(adMobVerifier.verify(anyString(), anyString(), anyLong())).willReturn(true);
            given(historyRepository.existsByTransactionId(TX_ID)).willReturn(false);
            // 시간 하드코딩 우회를 위해 any(LocalDateTime.class) 사용
            given(historyRepository.countDailyInkAds(eq(USER_ID), eq(AdRewardType.INK), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(5); // 한도(10) 미달
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(dummyUser));

            InkLog dummyLog = InkLog.builder().build();
            given(inkLogUtil.addInkAndGetInkLog(eq(dummyUser), eq(2), eq(InkLogType.WATCH_AD))).willReturn(dummyLog);

            // ======== When ========
            adsService.adMobCallbackInk("query", "valid_sig", "123", TX_ID, USER_ID.toString(), null, "INK", AD_UNIT_ID);

            // ======== Then ========
            verify(inkLogRepository, times(1)).save(any());
            verify(historyRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("[성공] WEEK_COUNSEL 보상 - TEST_PASS 서명 시 검증 패스 및 잠금 해제")
        void successWeekCounselRewardWithTestPass() {
            // ======== Given ========
            given(historyRepository.existsByTransactionId(TX_ID)).willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(dummyUser));
            given(weeklyCounselRepository.findByIdAndUser(COUNSEL_ID, dummyUser)).willReturn(Optional.of(dummyCounsel));

            // ======== When ========
            adsService.adMobCallbackInk("query", "TEST_PASS", "123", TX_ID, USER_ID.toString(), COUNSEL_ID.toString(), "WEEK_COUNSEL", AD_UNIT_ID);

            // ======== Then ========
            verify(adMobVerifier, never()).verify(anyString(), anyString(), anyLong()); // 검증 스킵 확인
            assertThat(dummyCounsel.isLocked()).isFalse(); // 잠금 해제 확인
            verify(historyRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("[무시] 이미 존재하는 트랜잭션 ID인 경우 로직 조기 종료")
        void ignoreDuplicateTransaction() {
            // ======== Given ========
            given(historyRepository.existsByTransactionId(TX_ID)).willReturn(true);

            // ======== When ========
            adsService.adMobCallbackInk("query", "TEST_PASS", "123", TX_ID, USER_ID.toString(), null, "INK", AD_UNIT_ID);

            // ======== Then ========
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("[무시] INK 보상 - 일일 한도 초과 시 보상 미지급")
        void ignoreExceedDailyLimit() {
            // ======== Given ========
            given(historyRepository.existsByTransactionId(TX_ID)).willReturn(false);
            given(historyRepository.countDailyInkAds(eq(USER_ID), eq(AdRewardType.INK), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(10); // 일일 한도(10) 도달

            // ======== When ========
            adsService.adMobCallbackInk("query", "TEST_PASS", "123", TX_ID, USER_ID.toString(), null, "INK", AD_UNIT_ID);

            // ======== Then ========
            verify(userRepository, never()).findById(anyLong());
            verify(inkLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패] 잘못된 서명인 경우 SecurityException 발생")
        void failInvalidSignature() {
            // ======== Given ========
            given(adMobVerifier.verify(anyString(), anyString(), anyLong())).willReturn(false);

            // ======== When & Then ========
            assertThatThrownBy(() -> adsService.adMobCallbackInk("query", "invalid_sig", "123", TX_ID, USER_ID.toString(), null, "INK", AD_UNIT_ID))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("AdMob Signature Failed");
        }
    }

    @Nested
    @DisplayName("grantTestAdReward() 테스트")
    class GrantTestAdRewardTest {

        @Test
        @DisplayName("[실패] 존재하지 않는 보상 타입 요청 시 예외 발생")
        void failUndefinedRewardType() {
            // ======== Given ========
            TestAdRewardReqDto reqDto = new TestAdRewardReqDto("INVALID_TYPE", 1L, AD_UNIT_ID);

            // ======== When & Then ========
            assertThatThrownBy(() -> adsService.grantTestAdReward(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(AdsErrorCode.UNDEFINED_AD_REWARD_TYPE.getMessage()); // Enum 메시지에 맞게 조정
        }
    }

    @Nested
    @DisplayName("getUserAdStatus() 테스트")
    class GetUserAdStatusTest {

        @Test
        @DisplayName("[성공] 한도 미달 시 광고 시청 가능 상태 반환")
        void successStatusAvailable() {
            // ======== Given ========
            given(historyRepository.countDailyInkAds(eq(USER_ID), eq(AdRewardType.INK), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(3);

            // ======== When ========
            UserAdStatusResDto result = adsService.getUserAdStatus(USER_ID);

            // ======== Then ========
            assertThat(result.currentViewCount()).isEqualTo(3);
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.message()).isEqualTo("광고 보기");
        }

        @Test
        @DisplayName("[성공] 한도 도달 시 광고 시청 불가 상태 반환")
        void successStatusUnavailable() {
            // Given
            given(historyRepository.countDailyInkAds(eq(USER_ID), eq(AdRewardType.INK), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(10);

            // When
            UserAdStatusResDto result = adsService.getUserAdStatus(USER_ID);

            // Then
            assertThat(result.currentViewCount()).isEqualTo(10);
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.message()).isEqualTo("오늘은 더이상 광고를 볼 수 없어요");
        }
    }
}