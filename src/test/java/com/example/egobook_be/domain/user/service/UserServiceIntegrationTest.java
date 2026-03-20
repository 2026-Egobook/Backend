package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.dto.req.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.auth.sevice.AuthService;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.enums.TermType;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // application-test.yml 환경 사용
public class UserServiceIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private TermRepository termRepository;
    @Autowired private RedisUtil redisUtil;
    @Autowired private HashingUtil hashingUtil;
    @Autowired private RedisTemplate<?, ?> redisTemplate;

    @Nested
    @DisplayName("withDrawAccount() 회원 탈퇴 메서드 통합 테스트")
    class WithDrawAccountTest {
        private final String DEVICE_UID = "withdraw-guest-device-uid";
        private User targetUser;
        private JwtTokenResDto userTokens;

        @BeforeEach
        void setUp() {
            // 1. 회원가입에 필요한 필수 기초 데이터 세팅
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());

            // 2. 실제 비즈니스 로직을 통한 Guest 사용자 가입 및 토큰 발급
            GuestJoinReqDto reqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();
            userTokens = authService.registerGuest(reqDto);

            // 3. 생성된 유저 엔티티 확보
            String hashedDeviceUid = hashingUtil.hashingValue(DEVICE_UID);
            AuthAccount authAccount = authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)
                    .orElseThrow(() -> new IllegalStateException("테스트용 유저 계정을 찾을 수 없습니다."));
            targetUser = authAccount.getUser();
        }

        @Test
        @DisplayName("[성공] 정상적인 회원 탈퇴 요청 시 데이터 비식별화 및 캐시(토큰) 무효화 검증")
        void successWithDrawAccount() {
            // ========= Given =========
            Long userId = targetUser.getId();
            String accessTokenWithBearer = "Bearer " + userTokens.accessToken();
            String hashedRefreshToken = hashingUtil.hashingValue(userTokens.refreshToken());

            // 탈퇴 전: Redis에 Refresh Token이 정상적으로 존재하는지 확인
            assertThat(redisUtil.getHashedRefreshTokenValue(hashedRefreshToken)).isNotNull();

            // ========= When =========
            // 회원 탈퇴 로직 실행
            userService.withDrawAccount(userId, accessTokenWithBearer);

            // ========= Then =========
            // 1. DB (영속성 컨텍스트) 반영 상태 검증
            User withdrawUser = userRepository.findById(userId).orElseThrow();
            AuthAccount withdrawAuthAccount = authAccountRepository.findByUser(withdrawUser).orElseThrow();

            // 1-1. Soft Delete 및 비식별화 확인
            assertThat(withdrawUser.getStatus()).isEqualTo(UserStatus.WITHDRAW_PENDING);
            assertThat(withdrawUser.getDeletedAt()).isNotNull();
            assertThat(withdrawUser.getPurgeAt()).isNotNull();
            assertThat(withdrawUser.getDailyPraise()).isFalse(); // AI 칭찬서 비활성화
            assertThat(withdrawUser.isNotificationEnabled()).isFalse(); // 알림 비활성화

            // 1-2. DB 백업 토큰 연관관계 해제(혹은 고아 객체 삭제) 검증
            assertThat(refreshTokenBackupRepository.findAllByAuthAccount(withdrawAuthAccount)).isEmpty();

            // 2. Redis 데이터 검증
            // 2-1. 기존 Access Token이 완벽하게 블랙리스트로 이동했는지 검증
            assertThat(redisUtil.checkTokenInBlacklist(userTokens.accessToken())).isTrue();

            // 2-2. 기존 Refresh Token이 Redis에서 완벽하게 파기되었는지 검증
            assertThat(redisUtil.getHashedRefreshTokenValue(hashedRefreshToken)).isNull();
        }

        @Test
        @DisplayName("[실패] 이미 탈퇴 대기(WITHDRAW_PENDING) 상태인 사용자가 중복 탈퇴 요청 시 예외 발생")
        void failAlreadyWithdrawPending() {
            // ========= Given =========
            Long userId = targetUser.getId();
            String accessTokenWithBearer = "Bearer " + userTokens.accessToken();

            // 첫 번째 탈퇴 요청으로 상태를 WITHDRAW_PENDING으로 변경
            userService.withDrawAccount(userId, accessTokenWithBearer);

            // ========= When & Then =========
            // 두 번째 탈퇴 요청 시 멱등성 보장 로직에 의해 예외가 발생해야 함
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.withDrawAccount(userId, accessTokenWithBearer);
            });
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ALREADY_WITHDRAW_PENDING);
        }

        @Test
        @DisplayName("[실패] DB에 존재하지 않는 잘못된 유저 ID로 탈퇴 요청 시 예외 발생")
        void failUserNotFound() {
            // ========= Given =========
            Long invalidUserId = 99999L; // 존재하지 않는 ID
            String accessTokenWithBearer = "Bearer " + userTokens.accessToken();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.withDrawAccount(invalidUserId, accessTokenWithBearer);
            });
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }

        @AfterEach
        void tearDownRedis() {
            // 다른 테스트 격리를 위한 Redis 초기화
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }
}