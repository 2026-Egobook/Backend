package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.*;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.enums.TermErrorCode;
import com.example.egobook_be.domain.terms.enums.TermType;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.UserNicknameGenerator;
import com.example.egobook_be.global.util.module.RedisValue;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // application-test.yml
public class AuthServiceIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private HashingUtil hashingUtil;
    @Autowired private RedisUtil redisUtil;
    @Autowired private TermRepository termRepository;
    @Autowired private AbilityRepository abilityRepository;
    @Autowired private UserTermRepository userTermRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private RedisTemplate<?, ?> redisTemplate;

    @MockitoBean // 외부 API인 구글 검증 로직만 Mocking 처리 (@MockBean은 deprecated 되었음)
    private GoogleIdTokenVerifier verifier;

    @Nested
    @DisplayName("registerGoogle() 메서드 테스트")
    class RegisterGoogleTest{
        private final String GOOGLE_SUB = "integration-test-sub";
        private final String EMAIL = "test@egobook.com";
        private final String ID_TOKEN = "dummy-id-token";

        /*
         * 모든 테스트 실행 전, AuthService에서 사용되는 GoogleIdTokenVerifier의 로직에서 verifier가 Mock GoogleIdToken 데이터를 반환하도록 설정한다.
         * -> 실제 구글 id token을 받아올 수 없으므로 해당 객체의 동작에서 Mock 데이터를 반환하도록 설정하는 것이다.
         */

        @BeforeEach
        void setUp() throws Exception{
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(GOOGLE_SUB);
            payload.setEmail(EMAIL);
            GoogleIdToken mockToken = new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
            given(verifier.verify(anyString())).willReturn(mockToken);
        }

        @Test
        @DisplayName("[성공] 사용자 구글 회원가입")
        public void successRegisterGoogle(){
            // ============= Given =============
            // 1. 사용자 구글 회원가입 시 필요한 데이터 세팅 (Item, Term)
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());
            GoogleJoinReqDto reqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();

            // ============= When =============
            JwtTokenResDto resDto = authService.registerGoogle(reqDto);

            // ============= Then =============
            // 1. DTO 반환 확인
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();

            // 2. DB에 잘 저장되었는지 확인
            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(authAccountRepository.count()).isEqualTo(1);
            assertThat(refreshTokenBackupRepository.count()).isEqualTo(1);

            // 3. Redis에 잘 저장되었는지 확인
            String hashedRefreshToken = hashingUtil.hashingValue(resDto.refreshToken()); // Key
            RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken); // Value

            assertThat(redisValue).isNotNull();
            assertThat(redisValue.subject()).isEqualTo(Provider.GOOGLE + ":" + hashingUtil.hashingValue(GOOGLE_SUB));
        }

        @Test
        @DisplayName("[성공] 탈퇴 대기(WITHDRAW_PENDING) 구글 사용자 재가입 시 ACTIVE 상태 복구")
        public void successReRegisterPendingGoogleUser() {
            // ============= Given =============
            // 1. 이미 가입되어 탈퇴 대기 중인 사용자(WITHDRAW_PENDING)를 DB에 미리 세팅
            User pendingUser = User.builder()
                    .accountCode("pending123")
                    .nickname("PendingUser")
                    .status(UserStatus.WITHDRAW_PENDING) // 핵심: 탈퇴 대기 상태
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            userRepository.save(pendingUser);

            String hashedGoogleSub = hashingUtil.hashingValue(GOOGLE_SUB);
            AuthAccount authAccount = AuthAccount.builder()
                    .provider(Provider.GOOGLE)
                    .hashedDeviceUid(hashedGoogleSub)
                    .user(pendingUser)
                    .build();
            authAccountRepository.save(authAccount);

            // 2. 재가입 요청 DTO 세팅
            GoogleJoinReqDto reqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();

            // ============= When =============
            JwtTokenResDto resDto = authService.registerGoogle(reqDto);

            // ============= Then =============
            // 1. DTO 반환 확인 (토큰이 정상적으로 재발급되었는지)
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();

            // 2. DB에서 유저를 다시 조회하여 실제 상태가 변경되었는지 확인 (Dirty Checking 검증)
            User restoredUser = userRepository.findById(pendingUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            assertThat(restoredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // 3. User 엔티티가 새로 생성된 것이 아니라 기존 엔티티가 업데이트 된 것인지 검증
            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(authAccountRepository.count()).isEqualTo(1);
        }

        @AfterEach
        void tearDownRedis(){
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

    @Nested
    @DisplayName("registerGuest() 메서드 테스트")
    class RegisterGuestTest{
        private final String DEVICE_UID = "test-device-uid";

        @Test
        @DisplayName("[성공] 사용자 Guest 회원가입")
        void successRegisterGuest(){
            // ========= Given =========
            GuestJoinReqDto reqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();

            // ========= When =========
            JwtTokenResDto resDto = authService.registerGuest(reqDto);

            // ========= Then =========
            // 1. Dto 반환 결과 확인
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();

            // 2. DB 확인
            // (User, UserItems, Ability, UserTerms, Mission, AuthAccount, RefreshTokenBackup)
            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(userItemRepository.count()).isNotEqualTo(0); // ItemInitializer로 Item이 초기화되고, 회원가입 시 기본 아이템은 여러개가 설정된다
            assertThat(abilityRepository.count()).isEqualTo(1);
            assertThat(userTermRepository.count()).isNotEqualTo(0);  // TermInitializer로 Term이 초기화되고, 회원가입 시 약관은 여러개가 설정된다
            assertThat(missionRepository.count()).isEqualTo(1);
            assertThat(authAccountRepository.count()).isEqualTo(1);
            assertThat(refreshTokenBackupRepository.count()).isEqualTo(1);

            // 3. Redis에 잘 저장되었는지 확인 - key: 해싱된 refresh token, value: 해당 사용자에 대한 메타 데이터
            String hashedRefreshToken = hashingUtil.hashingValue(resDto.refreshToken()); // Key
            RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken); // Value

            assertThat(redisValue).isNotNull();
            assertThat(redisValue.subject()).isEqualTo(Provider.GUEST + ":" + hashingUtil.hashingValue(DEVICE_UID));
        }

        @Test
        @DisplayName("[실패] 이미 존재하는 사용자 Guest 회원가입")
        void failExistingRegisterGuest(){
            // ======== Given ========
            // 1. device_uid로 먼저 User 가입
            GuestJoinReqDto reqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();
            authService.registerGuest(reqDto); // 첫 번째 가입 (성공)
            // 예외 발생 전 DB의 데이터 개수를 기록
            long userCountBefore = userRepository.count();
            long authAccountCountBefore = authAccountRepository.count();

            // ======== When & Then ========
            // 2. 동일한 DEVICE_UID로 다시 가입 시도하여 예외가 터지는지 확인
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.registerGuest(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_REGISTERED_USER);

            // 3. DB 롤백 검증: 예외가 터졌으므로 데이터 개수가 이전과 완벽히 동일해야 함.
            assertThat(userRepository.count()).isEqualTo(userCountBefore);
            assertThat(authAccountRepository.count()).isEqualTo(authAccountCountBefore);
        }


        @AfterEach
        void tearDownRedis(){
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

    @Nested
    @DisplayName("refreshToken() 메서드 테스트")
    class RefreshTokenTest {
        private final String DEVICE_UID = "refresh-test-device-uid";
        private JwtTokenResDto initialTokens;

        @BeforeEach
        void setUp() {
            // 회원가입 시 할당되는 기본 아이템 및 약관 세팅
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());

            // 1. 테스트 진행을 위한 정상 Guest 사용자 가입 및 토큰 발급
            GuestJoinReqDto reqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();
            initialTokens = authService.registerGuest(reqDto);
        }

        @Test
        @DisplayName("[성공] Redis에서 Refresh Token 조회 성공 시 AccessToken 정상 재발급")
        void successRefreshTokenFromRedis() {
            // ========= Given =========
            RefreshReqDto reqDto = RefreshReqDto.builder()
                    .accessToken(initialTokens.accessToken())
                    .refreshToken(initialTokens.refreshToken())
                    .build();

            // ========= When =========
            JwtTokenResDto resDto = authService.refreshToken(reqDto);

            // ========= Then =========
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();
            assertThat(resDto.accessToken()).isNotEqualTo(initialTokens.accessToken()); // Access Token이 갱신되었는지 확인
            assertThat(resDto.refreshToken()).isEqualTo(initialTokens.refreshToken()); // Refresh Token은 그대로 유지

            // 기존 Access Token이 블랙리스트에 정상적으로 등록되었는지 확인
            assertThat(redisUtil.checkTokenInBlacklist(initialTokens.accessToken())).isTrue();
        }

        @Test
        @DisplayName("[성공] Redis에서 실패 후, DB(RefreshTokenBackup) Fallback 조회하여 성공 및 Redis 복구")
        void successRefreshTokenFromDatabaseFallback() {
            // ========= Given =========
            RefreshReqDto reqDto = RefreshReqDto.builder()
                    .accessToken(initialTokens.accessToken())
                    .refreshToken(initialTokens.refreshToken())
                    .build();

            // 강제로 Redis 데이터를 모두 날려서 Redis Miss 상황 연출
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

            // ========= When =========
            JwtTokenResDto resDto = authService.refreshToken(reqDto);

            // ========= Then =========
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();

            // Redis에 Refresh Token 정보가 복구되었는지 확인
            String hashedRefreshToken = hashingUtil.hashingValue(initialTokens.refreshToken());
            RedisValue recoveredValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken);
            assertThat(recoveredValue).isNotNull();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 잘못된 Refresh Token으로 요청 시 예외 발생")
        void failRefreshTokenNotFound() {
            // ========= Given =========
            RefreshReqDto reqDto = RefreshReqDto.builder()
                    .accessToken(initialTokens.accessToken())
                    .refreshToken("invalid-refresh-token")
                    .build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.refreshToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패] 만료된 Guest Refresh Token으로 요청 시 예외 발생")
        void failGuestRefreshTokenExpired() {
            // ========= Given =========
            RefreshReqDto reqDto = RefreshReqDto.builder()
                    .accessToken(initialTokens.accessToken())
                    .refreshToken(initialTokens.refreshToken())
                    .build();

            // 강제로 Redis 데이터 삭제
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

            // DB에 있는 Refresh Token의 만료 시간을 과거로 조작
            RefreshTokenBackup backup = refreshTokenBackupRepository.findAll().get(0);
            backup.updateBackupInfo(backup.getHashedDeviceUid(), backup.getHashedTokenValue(), LocalDateTime.now().minusDays(1));
            refreshTokenBackupRepository.saveAndFlush(backup);

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.refreshToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED_GUEST);
        }

        @AfterEach
        void tearDownRedis(){
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

    @Nested
    @DisplayName("recertificationGuestToken() 메서드 테스트")
    class RecertificationGuestTokenTest {
        private final String DEVICE_UID = "recert-guest-device-uid";
        private JwtTokenResDto initialTokens;

        @BeforeEach
        void setUp() {
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());

            GuestJoinReqDto reqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();
            initialTokens = authService.registerGuest(reqDto);
        }

        @Test
        @DisplayName("[성공] Guest Refresh/Recover Token 정상 재발급 및 세션 정보 갱신")
        void successRecertificationGuestToken() {
            // ========= Given =========
            GuestRecertificationReqDto reqDto = GuestRecertificationReqDto.builder()
                    .deviceUid(DEVICE_UID)
                    .accessToken(initialTokens.accessToken())
                    .recoverToken(initialTokens.recoverToken())
                    .build();

            // ========= When =========
            JwtTokenResDto resDto = authService.recertificationGuestToken(reqDto);

            // ========= Then =========
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();
            assertThat(resDto.refreshToken()).isNotBlank();
            assertThat(resDto.recoverToken()).isNotBlank();

            // 발급된 토큰들이 이전 토큰들과 다른 새로운 토큰인지 검증
            assertThat(resDto.accessToken()).isNotEqualTo(initialTokens.accessToken());
            assertThat(resDto.refreshToken()).isNotEqualTo(initialTokens.refreshToken());
            assertThat(resDto.recoverToken()).isNotEqualTo(initialTokens.recoverToken());

            // 기존 Access Token은 블랙리스트 처리 되었는지 확인
            assertThat(redisUtil.checkTokenInBlacklist(initialTokens.accessToken())).isTrue();

            // 기존 Refresh Token은 Redis에서 삭제되었는지 확인
            String oldHashedRefreshToken = hashingUtil.hashingValue(initialTokens.refreshToken());
            assertThat(redisUtil.getHashedRefreshTokenValue(oldHashedRefreshToken)).isNull();
        }

        @Test
        @DisplayName("[실패] Recover Token이 DB 값과 일치하지 않을 때 사용자 상태 WITHDRAW_PENDING 변경 및 예외 발생")
        void failInvalidRecoverToken() {
            // ========= Given =========
            GuestRecertificationReqDto reqDto = GuestRecertificationReqDto.builder()
                    .deviceUid(DEVICE_UID)
                    .accessToken(initialTokens.accessToken())
                    .recoverToken("wrong-recover-token")
                    .build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGuestToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_RECOVER_TOKEN);

            // 사용자 상태가 삭제 대기(WITHDRAW_PENDING)로 변경되었는지 영속성 컨텍스트를 비우고 확인
            User user = userRepository.findAll().get(0);
            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAW_PENDING);
        }

        @Test
        @DisplayName("[실패] 삭제 대기 중인 사용자가 재인증 시도할 경우 예외 발생")
        void failUserWithdrawPending() {
            // ========= Given =========
            User user = userRepository.findAll().get(0);
            user.withdrawUser(1000L * 60 * 60 * 24 * 30); // 30일 후 삭제되도록 설정(WITHDRAW_PENDING 변경)
            userRepository.saveAndFlush(user);

            GuestRecertificationReqDto reqDto = GuestRecertificationReqDto.builder()
                    .deviceUid(DEVICE_UID)
                    .accessToken(initialTokens.accessToken())
                    .recoverToken(initialTokens.recoverToken())
                    .build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGuestToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

        @AfterEach
        void tearDownRedis(){
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

    @Nested
    @DisplayName("recertificationGoogleToken() 메서드 테스트")
    class RecertificationGoogleTokenTest {
        private final String GOOGLE_SUB = "recert-google-sub";
        private final String EMAIL = "recert@egobook.com";
        private final String ID_TOKEN = "dummy-id-token";
        private JwtTokenResDto initialTokens;

        @BeforeEach
        void setUp() throws Exception {
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(GOOGLE_SUB);
            payload.setEmail(EMAIL);
            GoogleIdToken mockToken = new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
            given(verifier.verify(anyString())).willReturn(mockToken);

            GoogleJoinReqDto reqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();
            initialTokens = authService.registerGoogle(reqDto);
        }

        @Test
        @DisplayName("[성공] Google 사용자 재인증 시 Access/Refresh Token 정상 재발급")
        void successRecertificationGoogleToken() {
            // ========= Given =========
            GoogleRecertificationReqDto reqDto = GoogleRecertificationReqDto.builder()
                    .idToken(ID_TOKEN)
                    .accessToken(initialTokens.accessToken())
                    .build();

            // ========= When =========
            JwtTokenResDto resDto = authService.recertificationGoogleToken(reqDto);

            // ========= Then =========
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();
            assertThat(resDto.refreshToken()).isNotBlank();
            assertThat(resDto.recoverToken()).isNull(); // 구글 재인증은 Recover Token 발급 안함

            // 기존 Access Token 블랙리스트 검증
            assertThat(redisUtil.checkTokenInBlacklist(initialTokens.accessToken())).isTrue();

            // 기존 Refresh Token Redis 삭제 검증
            String oldHashedRefreshToken = hashingUtil.hashingValue(initialTokens.refreshToken());
            assertThat(redisUtil.getHashedRefreshTokenValue(oldHashedRefreshToken)).isNull();
        }

        @Test
        @DisplayName("[실패] 등록되지 않은 구글 계정으로 재인증 시도 시 예외 발생")
        void failGoogleUserNotFound() throws Exception {
            // ========= Given =========
            // 다른 sub를 가진 Payload 생성 (DB에 없는 사용자)
            GoogleIdToken.Payload unknownPayload = new GoogleIdToken.Payload();
            unknownPayload.setSubject("unknown-sub-1234");
            GoogleIdToken unknownMockToken = new GoogleIdToken(new JsonWebSignature.Header(), unknownPayload, new byte[0], new byte[0]);
            given(verifier.verify(anyString())).willReturn(unknownMockToken);

            GoogleRecertificationReqDto reqDto = GoogleRecertificationReqDto.builder()
                    .idToken("unknown-id-token")
                    .accessToken(initialTokens.accessToken())
                    .build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGoogleToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패] 삭제 대기 중인 구글 사용자가 재인증 시도할 경우 예외 발생")
        void failGoogleUserWithdrawPending() {
            // ========= Given =========
            User user = userRepository.findAll().get(0);
            user.withdrawUser(1000L * 60 * 60 * 24 * 30); // WITHDRAW_PENDING으로 상태 변경
            userRepository.saveAndFlush(user);

            GoogleRecertificationReqDto reqDto = GoogleRecertificationReqDto.builder()
                    .idToken(ID_TOKEN)
                    .accessToken(initialTokens.accessToken())
                    .build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGoogleToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

        @AfterEach
        void tearDownRedis(){
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

    @Nested
    @DisplayName("linkGoogleAccount() 메서드 테스트")
    class LinkGoogleAccountTest {
        private final String DEVICE_UID = "link-guest-device-uid";
        private final String GOOGLE_SUB = "link-google-sub";
        private final String EMAIL = "link-test@egobook.com";
        private final String ID_TOKEN = "dummy-link-id-token";

        private JwtTokenResDto initialTokens;
        private User guestUser;

        @BeforeEach
        void setUp() throws Exception {
            // 1. 필수 초기화 데이터 세팅 (테스트 독립성을 위해 저장)
            itemRepository.save(Item.builder().path("/test").category(ItemCategory.BACK).name("test").build());
            termRepository.save(Term.builder().termType(TermType.TERM_OF_SERVICE).description("test").context("test").required(true).build());

            // 2. Google OAuth 검증 Mocking 세팅 (연동 시 사용될 구글 계정 정보)
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(GOOGLE_SUB);
            payload.setEmail(EMAIL);
            GoogleIdToken mockToken = new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
            given(verifier.verify(anyString())).willReturn(mockToken);

            // 3. 연동을 시도할 주체인 Guest 사용자 실제 회원가입
            GuestJoinReqDto guestReqDto = GuestJoinReqDto.builder().deviceUid(DEVICE_UID).build();
            initialTokens = authService.registerGuest(guestReqDto);

            // 4. 방금 가입한 Guest 유저 엔티티를 조회
            String hashedDeviceUid = hashingUtil.hashingValue(DEVICE_UID);
            AuthAccount guestAccount = authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)
                    .orElseThrow(() -> new IllegalStateException("Guest AuthAccount를 찾을 수 없습니다."));
            guestUser = guestAccount.getUser();
        }

        @Test
        @DisplayName("[성공] Guest 사용자 Google 계정 연동 성공 및 데이터 마이그레이션 확인")
        void successLinkGoogleAccount() {
            // ========= Given =========
            GoogleJoinReqDto reqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();

            // ========= When =========
            JwtTokenResDto resDto = authService.linkGoogleAccount(guestUser.getId(), reqDto);

            // ========= Then =========
            // 1. 반환된 DTO 토큰 검증
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isNotBlank();
            assertThat(resDto.refreshToken()).isNotBlank();
            assertThat(resDto.recoverToken()).isNull(); // Google 연동이므로 Recover Token은 발급되지 않아야 함
            assertThat(resDto.email()).isEqualTo(EMAIL);

            // 발급된 토큰들이 기존 Guest 토큰과 다른 새로운 토큰인지 검증
            assertThat(resDto.accessToken()).isNotEqualTo(initialTokens.accessToken());
            assertThat(resDto.refreshToken()).isNotEqualTo(initialTokens.refreshToken());

            // 2. DB 상태 검증 (마이그레이션 확인)
            // 2-1. User 정보 업데이트 확인 (이메일이 들어갔는지)
            User updatedUser = userRepository.findById(guestUser.getId()).orElseThrow();
            assertThat(updatedUser.getEmail()).isEqualTo(EMAIL);

            // 2-2. AuthAccount 정보 업데이트 확인 (GUEST -> GOOGLE 로 완전히 전환되었는지)
            AuthAccount updatedAuthAccount = authAccountRepository.findByUserIdAndProvider(guestUser.getId(), Provider.GOOGLE)
                    .orElseThrow(() -> new IllegalStateException("Google 연동 계정을 찾을 수 없습니다."));
            assertThat(updatedAuthAccount.getProvider()).isEqualTo(Provider.GOOGLE);
            assertThat(updatedAuthAccount.getHashedDeviceUid()).isEqualTo(hashingUtil.hashingValue(GOOGLE_SUB));
            assertThat(updatedAuthAccount.getHashedRecoverToken()).isNull();

            // 3. Redis 상태 검증
            // 3-1. 기존 Guest Refresh Token이 Redis에서 확실히 지워졌는지 확인
            String oldHashedRefreshToken = hashingUtil.hashingValue(initialTokens.refreshToken());
            assertThat(redisUtil.getHashedRefreshTokenValue(oldHashedRefreshToken)).isNull();

            // 3-2. 새로운 Google Refresh Token이 Redis에 정상 등록되었는지 확인
            String newHashedRefreshToken = hashingUtil.hashingValue(resDto.refreshToken());
            RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(newHashedRefreshToken);
            assertThat(redisValue).isNotNull();
            assertThat(redisValue.subject()).isEqualTo(Provider.GOOGLE + ":" + hashingUtil.hashingValue(GOOGLE_SUB));
        }

        @Test
        @DisplayName("[실패] 이미 다른 계정에서 연동하여 사용 중인 구글 계정으로 연동 시도 시 예외 발생")
        void failAlreadyRegisteredGoogleUser() {
            // ========= Given =========
            GoogleJoinReqDto googleReqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();

            // 1. 누군가 구글 계정(GOOGLE_SUB)으로 먼저 회원가입을 한 상태라고 가정
            authService.registerGoogle(googleReqDto);

            // ========= When & Then =========
            // 2. Guest 사용자가 해당 구글 계정으로 연동을 시도하면 예외 발생
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.linkGoogleAccount(guestUser.getId(), googleReqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_REGISTERED_USER);

            // 3. 예외 발생 시, 기존 Guest 계정 정보가 날아가지 않고 그대로 유지되는지 롤백 검증
            AuthAccount guestAccount = authAccountRepository.findByUserIdAndProvider(guestUser.getId(), Provider.GUEST).orElse(null);
            assertThat(guestAccount).isNotNull();
            assertThat(guestAccount.getProvider()).isEqualTo(Provider.GUEST);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 사용자의 PK(Guest 계정 없음)로 연동 시도 시 예외 발생")
        void failGuestAccountNotFound() {
            // ========= Given =========
            Long invalidUserId = 99999L; // DB에 존재하지 않는 임의의 유저 ID
            GoogleJoinReqDto reqDto = GoogleJoinReqDto.builder().idToken(ID_TOKEN).build();

            // ========= When & Then =========
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.linkGoogleAccount(invalidUserId, reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }

        @AfterEach
        void tearDownRedis() {
            // 테스트 종료 후 Redis 깔끔하게 초기화
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            }
        }
    }

}
