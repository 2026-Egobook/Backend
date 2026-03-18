package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.*;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.service.UserService;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.security.CustomUserDetails;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.module.RedisValue;
import com.example.egobook_be.global.util.module.TokenInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {
    @InjectMocks
    private AuthService authService;

    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private TokenManagementService tokenManagementService;
    @Mock private UserService userService;

    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private HashingUtil hashingUtil;
    @Mock private RedisUtil redisUtil;

    @Nested
    @DisplayName("registerGoogle() 메서드 테스트")
    class RegisterGoogleTest{
        @Test
        @DisplayName("[성공1] 신규 구글 사용자 가입")
        void successRegisterGoogleUser(){
            // ========= Given =========
            String idToken = "valid.google.id.token";
            String googleSub = "google-sub-12345";
            String email = "test@gmail.com";
            String hashedGoogleSub = "hashed-google-sub-12345";
            String accountCode = "abcde1234";

            GoogleJoinReqDto reqDto = new GoogleJoinReqDto(idToken);

            /*
             * 1. 구글 토큰 파싱 결과 Mocking & Stub
             * - googleOAuthService.verifyToken(reqDto.idToken())가 실행되면 기존 prod 코드처럼 payload를 반환하도록 Stub 설정
             * - hashingUtil.hashingValue(googleSub)가 실행되면 hashedGoogleSub값 반환하도록 Stub 설정
             */
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);
            payload.setEmail(email);
            given(googleOAuthService.verifyToken(reqDto.idToken())).willReturn(payload);
            given(hashingUtil.hashingValue(googleSub)).willReturn(hashedGoogleSub);

            // 2. 신규 사용자의 가입을 테스트하므로 DB에 기존 계정이 없다는 상태를 반환하도록 Stub 설정 (내가 null을 반환하도록 직접 설정하면 안되고, 프로덕션 코드에서 orElse로 null을 반환하도록 해야함)
            given(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)).willReturn(Optional.empty());

            // 3. 신규 User Entity 생성
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname("에고북1234")
                    .lastLoginAt(LocalDateTime.now())
                    .email(email)
                    .build();
            given(userService.initializeAndRegisterUser(anyString())).willReturn(mockUser);

            /*
             * 4. createAuthAccount()
             * - MockAuthAccount 객체 생성
             * - authAccountRepository.save(authAccount) return Stub 설정
             */
            AuthAccount mockAuthAccount = AuthAccount.builder()
                    .id(2L)
                    .provider(Provider.GOOGLE)
                    .hashedDeviceUid(hashedGoogleSub)
                    .user(mockUser)
                    .build();
            given(authAccountRepository.save(any(AuthAccount.class))).willReturn(mockAuthAccount);

            /*
             * 5. processIssueTokens()
             * - jwtUtil.createAccessToken(), createRefreshToken() Mock, Stub 설정
             * - processRefreshTokenSaving - hashingUtil.hashingValue() Mock, Stub 설정
             * - processRefreshTokenSaving - jwtUtil.createSubject() Mock, Stub 설정
             * - updateRefreshTokenBackupTable - refreshTokenBackupRepository.existsByAuthAccount() Stub 설정
             */
            TokenInfo mockAccessTokenInfo = TokenInfo.builder().token("access-token-value").expiresAt(LocalDateTime.now().plusHours(1)).build();
            TokenInfo mockRefreshTokenInfo = TokenInfo.builder().token("refresh-token-value").expiresAt(LocalDateTime.now().plusHours(24)).build();

            given(jwtUtil.createAccessToken(any(CustomUserDetails.class))).willReturn(mockAccessTokenInfo);
            given(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).willReturn(mockRefreshTokenInfo);

            // ========= When =========
            JwtTokenResDto resDto = authService.registerGoogle(reqDto);

            // ========= Then =========
            // 1. 반환된 객체의 상태 값 검증
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isEqualTo("access-token-value");
            assertThat(resDto.refreshToken()).isEqualTo("refresh-token-value");
            assertThat(resDto.recoverToken()).isNull(); // 구글 가입은 Recover Token을 발급 안함
            assertThat(resDto.email()).isEqualTo(email);

            // 2. 핵심 로직들 1번씩 실행 되었는지 행위 검증
            verify(googleOAuthService, times(1)).verifyToken(reqDto.idToken());
            verify(userService, times(1)).initializeAndRegisterUser(email);
            verify(authAccountRepository, times(1)).save(any(AuthAccount.class));
            verify(tokenManagementService, times(1)).saveRefreshTokenToTableAndRedis(mockRefreshTokenInfo, mockUser, mockAuthAccount);
        }

        @Test
        @DisplayName("[성공 2] 삭제 대기 사용자 재가입")
        void successWithdrawPendingGoogleUserReRegister(){
            // ========= Given =========
            String idToken = "valid.google.id.token";
            String googleSub = "google-sub-12345";
            String email = "test@gmail.com";
            String hashedGoogleSub = "hashed-google-sub-12345";
            String accountCode = "abcde1234";

            GoogleJoinReqDto reqDto = new GoogleJoinReqDto(idToken);

            /*
             * 1. 구글 토큰 파싱 결과 Mocking & Stub
             * - googleOAuthService.verifyToken(reqDto.idToken())가 실행되면 기존 prod 코드처럼 payload를 반환하도록 Stub 설정
             * - hashingUtil.hashingValue(googleSub)가 실행되면 hashedGoogleSub값 반환하도록 Stub 설정
             */
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);
            payload.setEmail(email);
            given(googleOAuthService.verifyToken(reqDto.idToken())).willReturn(payload);
            given(hashingUtil.hashingValue(googleSub)).willReturn(hashedGoogleSub);

            // 2. 이미 DB에 기존에 가입된 계정이 있는 상태로 Mock, Stub 설정
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname("nickname")
                    .status(UserStatus.WITHDRAW_PENDING)
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            AuthAccount mockAuthAccount = AuthAccount.builder()
                    .id(1L)
                    .provider(Provider.GOOGLE)
                    .hashedDeviceUid(hashedGoogleSub)
                    .user(mockUser)
                    .build();
            given(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)).willReturn(Optional.of(mockAuthAccount));

            /*
             * 3. processIssueTokens()
             * - jwtUtil.createAccessToken(), createRefreshToken() Mock, Stub 설정
             * - processRefreshTokenSaving - hashingUtil.hashingValue() Mock, Stub 설정
             * - processRefreshTokenSaving - jwtUtil.createSubject() Mock, Stub 설정
             * - updateRefreshTokenBackupTable - refreshTokenBackupRepository.existsByAuthAccount() Stub 설정
             */
            TokenInfo mockAccessTokenInfo = TokenInfo.builder().token("access-token-value").expiresAt(LocalDateTime.now().plusHours(1)).build();
            TokenInfo mockRefreshTokenInfo = TokenInfo.builder().token("refresh-token-value").expiresAt(LocalDateTime.now().plusHours(24)).build();

            given(jwtUtil.createAccessToken(any(CustomUserDetails.class))).willReturn(mockAccessTokenInfo);
            given(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).willReturn(mockRefreshTokenInfo);

            // ========= When =========
            JwtTokenResDto resDto = authService.registerGoogle(reqDto);

            // ========= Then =========
            // 1. 반환된 객체의 상태 값 검증
            assertThat(resDto).isNotNull();
            assertThat(mockUser.getStatus()).isEqualTo(UserStatus.ACTIVE); // 사용자의 상태가 ACTIVE로 성공적으로 바뀌었는지 검증
            assertThat(resDto.accessToken()).isEqualTo("access-token-value");
            assertThat(resDto.refreshToken()).isEqualTo("refresh-token-value");
            assertThat(resDto.recoverToken()).isNull(); // 구글 가입은 Recover Token을 발급 안함
            assertThat(resDto.email()).isEqualTo(email);

            // 2. 핵심 로직들 1번씩 실행 되었는지 행위 검증
            verify(googleOAuthService, times(1)).verifyToken(reqDto.idToken()); // 구글 ID Token 검증 행위
            verify(tokenManagementService, times(1)).saveRefreshTokenToTableAndRedis(mockRefreshTokenInfo, mockUser, mockAuthAccount);

            // 3. 삭제 대기중인 사용자가 계정을 복구한 것이므로, 아래 2개의 로직은 실행되면 안된다
            verify(userService, never()).initializeAndRegisterUser(anyString());
            verify(authAccountRepository, never()).save(any(AuthAccount.class));
        }

        @Test
        @DisplayName("[실패 1] 이미 가입된 존재하는 사용자 재가입 시도")
        void failExistingGoogleUser(){
            // ========= Given =========
            String idToken = "valid.google.id.token";
            String googleSub = "google-sub-12345";
            String email = "test@gmail.com";
            String hashedGoogleSub = "hashed-google-sub-12345";
            String accountCode = "abcde1234";

            GoogleJoinReqDto reqDto = new GoogleJoinReqDto(idToken);

            /*
             * 1. 구글 토큰 파싱 결과 Mocking & Stub
             * - googleOAuthService.verifyToken(reqDto.idToken())가 실행되면 기존 prod 코드처럼 payload를 반환하도록 Stub 설정
             * - hashingUtil.hashingValue(googleSub)가 실행되면 hashedGoogleSub값 반환하도록 Stub 설정
             */
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);
            payload.setEmail(email);
            given(googleOAuthService.verifyToken(reqDto.idToken())).willReturn(payload);
            given(hashingUtil.hashingValue(googleSub)).willReturn(hashedGoogleSub);

            // 2. 이미 DB에 기존에 가입된 계정이 있는 상태로 Mock, Stub 설정
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname("nickname")
                    .status(UserStatus.ACTIVE)
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            AuthAccount mockAuthAccount = AuthAccount.builder()
                    .id(1L)
                    .provider(Provider.GOOGLE)
                    .hashedDeviceUid(hashedGoogleSub)
                    .user(mockUser)
                    .build();
            given(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)).willReturn(Optional.of(mockAuthAccount));

            // ========= When & Then =========
            // 1. authService.registerGoogle(reqDto)에서 CustomException이 발생하면 테스트 성공
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.registerGoogle(reqDto);
            });

            // 2. 발생한 Exception에 들어있는 내용이 AuthErrorCode.ALREADY_REGISTERED_USER와 같은지 확인
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_REGISTERED_USER);

            // 3. 예외가 터져서 로직이 중단되었으므로, DB 저장이나 토큰 발급 로직이 실행되지 않았음을 검증
            verify(userService, never()).initializeAndRegisterUser(anyString());
            verify(jwtUtil, never()).createAccessToken(any());
        }

        @Test
        @DisplayName("[실패 2] 완전 삭제 처리되었지만 아직 데이터가 남아있는 사용자 가입 시도")
        void failWithdrewGoogleUser(){
            // ========= Given =========
            String idToken = "valid.google.id.token";
            String googleSub = "google-sub-12345";
            String email = "test@gmail.com";
            String hashedGoogleSub = "hashed-google-sub-12345";
            String accountCode = "abcde1234";

            GoogleJoinReqDto reqDto = new GoogleJoinReqDto(idToken);

            /*
             * 1. 구글 토큰 파싱 결과 Mocking & Stub
             * - googleOAuthService.verifyToken(reqDto.idToken())가 실행되면 기존 prod 코드처럼 payload를 반환하도록 Stub 설정
             * - hashingUtil.hashingValue(googleSub)가 실행되면 hashedGoogleSub값 반환하도록 Stub 설정
             */
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);
            payload.setEmail(email);
            given(googleOAuthService.verifyToken(reqDto.idToken())).willReturn(payload);
            given(hashingUtil.hashingValue(googleSub)).willReturn(hashedGoogleSub);

            // 2. 이미 DB에 Status가 Withdraw인 계정이 있는 상태로 Mock, Stub 설정
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname("nickname")
                    .status(UserStatus.WITHDRAW)
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            AuthAccount mockAuthAccount = AuthAccount.builder()
                    .id(1L)
                    .provider(Provider.GOOGLE)
                    .hashedDeviceUid(hashedGoogleSub)
                    .user(mockUser)
                    .build();
            given(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)).willReturn(Optional.of(mockAuthAccount));

            // ========= When & Then =========
            // 1. authService.registerGoogle(reqDto)에서 CustomException이 발생하면 테스트 성공
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.registerGoogle(reqDto);
            });

            // 2. 발생한 Exception에 들어있는 내용이 AuthErrorCode.GOOGLE_JOIN_FAIL_USER_WITHDRAWN와 같은지 확인
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.GOOGLE_JOIN_FAIL_USER_WITHDRAWN);

            // 3. 예외가 터져서 로직이 중단되었으므로, DB 저장이나 토큰 발급 로직이 실행되지 않았음을 검증
            verify(userService, never()).initializeAndRegisterUser(anyString());
            verify(jwtUtil, never()).createAccessToken(any());
        }
    }

    @Nested
    @DisplayName("registerGuest() 메서드 테스트")
    class RegisterGuestTest{
        @Test
        @DisplayName("[성공] 신규 게스트 사용자 가입")
        void successRegisterGuestUser(){
            // ========= Given =========
            String deviceUid = "guest-device-uid-123";
            String hashedDeviceUid = "hashed-guest-device-uid-123";
            String accountCode = "guest1234";

            GuestJoinReqDto reqDto = new GuestJoinReqDto(deviceUid);

            // 1. DeviceUid 해싱 및 중복 가입 검증 Mocking
            given(hashingUtil.hashingValue(deviceUid)).willReturn(hashedDeviceUid);
            given(authAccountRepository.existsByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)).willReturn(false);

            // 2. 신규 User Entity 생성
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname("에고북1234")
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            given(userService.initializeAndRegisterUser(anyString())).willReturn(mockUser);

            // 3. createAuthAccount()
            AuthAccount mockAuthAccount = AuthAccount.builder()
                    .id(2L)
                    .provider(Provider.GUEST)
                    .hashedDeviceUid(hashedDeviceUid)
                    .user(mockUser)
                    .build();
            given(authAccountRepository.save(any(AuthAccount.class))).willReturn(mockAuthAccount);

            /*
             * 4. 토큰 발급 및 저장 (processIssueTokens 및 Recover Token)
             * - Guest 로직에서는 Recover Token과 Refresh Token 2개가 해싱되므로 각각에 대한 리턴값을 명시합니다.
             */
            TokenInfo mockAccessTokenInfo = TokenInfo.builder().token("access-token-value").expiresAt(LocalDateTime.now().plusHours(1)).build();
            TokenInfo mockRefreshTokenInfo = TokenInfo.builder().token("refresh-token-value").expiresAt(LocalDateTime.now().plusDays(14)).build();
            TokenInfo mockRecoverTokenInfo = TokenInfo.builder().token("recover-token-value").expiresAt(LocalDateTime.now().plusDays(30)).build();

            String hashedRecoverToken = "hashed-recover-token-value";

            given(jwtUtil.createAccessToken(any(CustomUserDetails.class))).willReturn(mockAccessTokenInfo);
            given(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).willReturn(mockRefreshTokenInfo);
            given(jwtUtil.createRecoverToken(any(CustomUserDetails.class))).willReturn(mockRecoverTokenInfo);

            // hashingUtil이 파라미터에 따라 다르게 리턴하도록 설정
            given(hashingUtil.hashingValue("recover-token-value")).willReturn(hashedRecoverToken);

            // ========= When =========
            JwtTokenResDto resDto = authService.registerGuest(reqDto);

            // ========= Then =========
            // 1. 반환된 객체의 상태 값 검증
            assertThat(resDto).isNotNull();
            assertThat(resDto.accessToken()).isEqualTo("access-token-value");
            assertThat(resDto.refreshToken()).isEqualTo("refresh-token-value");
            assertThat(resDto.recoverToken()).isEqualTo("recover-token-value"); // Guest는 Recover Token이 발급되어야 함
            assertThat(resDto.email()).isNull(); // Guest는 이메일이 없음

            // 2. 핵심 로직들 1번씩 실행 되었는지 행위 검증
            verify(userService, times(1)).initializeAndRegisterUser(hashedDeviceUid);
            verify(authAccountRepository, times(1)).save(any(AuthAccount.class));
            verify(tokenManagementService, times(1)).saveRefreshTokenToTableAndRedis(mockRefreshTokenInfo, mockUser, mockAuthAccount);

            assertThat(mockAuthAccount.getHashedRecoverToken()).isEqualTo(hashedRecoverToken);
        }

        @Test
        @DisplayName("[실패] 이미 Guest로 등록된 기기 가입 시도")
        void failExistingGuestUser(){
            // ========= Given =========
            String deviceUid = "existing-guest-device-uid";
            String hashedDeviceUid = "hashed-existing-guest-device-uid";

            GuestJoinReqDto reqDto = new GuestJoinReqDto(deviceUid);

            // 1. DeviceUid 해싱
            given(hashingUtil.hashingValue(deviceUid)).willReturn(hashedDeviceUid);

            // 2. 이미 해당 HashedDeviceUid와 GUEST Provider로 가입된 계정이 존재한다고 가정 (true 반환)
            given(authAccountRepository.existsByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)).willReturn(true);

            // ========= When & Then =========
            // 1. authService.registerGuest(reqDto) 호출 시 CustomException 발생 검증
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.registerGuest(reqDto);
            });

            // 2. 발생한 Exception의 에러 코드가 ALREADY_REGISTERED_USER 인지 확인
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_REGISTERED_USER);

            // 3. 예외가 발생했으므로 User 저장이나 토큰 생성 등 후속 로직이 전혀 실행되지 않았음을 행위 검증
            verify(userService, never()).initializeAndRegisterUser(anyString());
            verify(authAccountRepository, never()).save(any(AuthAccount.class));
            verify(jwtUtil, never()).createAccessToken(any());
            verify(tokenManagementService, never()).saveRefreshTokenToTableAndRedis(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("refreshToken() 메서드 테스트")
    class RefreshTokenTest {
        @Test
        @DisplayName("[성공 1] Redis에서 Refresh Token 조회 후 AccessToken 재발급")
        void successSearchRefreshTokenInRedis(){
            // given
            RefreshReqDto reqDto = new RefreshReqDto("oldAccessToken", "rawRefreshToken");
            String hashedToken = "hashedRefreshToken";

            RedisValue redisValue = RedisValue.builder()
                    .userId(1L)
                    .authAccountId(1L)
                    .subject("GUEST:deviceUid")
                    .role(RoleType.ROLE_USER)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));

            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(redisValue);
            when(jwtUtil.createAccessToken(1L, 1L, "GUEST:deviceUid", RoleType.ROLE_USER))
                    .thenReturn(newAccessTokenInfo);

            // when
            JwtTokenResDto resDto = authService.refreshToken(reqDto);

            // then
            // 1. resDto의 accessToken, refreshToken값에 Mock으로 넣은 데이터가 있고, recoverToken에는 값이 없는지 확인
            assertThat(resDto.accessToken()).isEqualTo("newAccessToken");
            assertThat(resDto.refreshToken()).isEqualTo("rawRefreshToken");
            assertThat(resDto.recoverToken()).isNull();

            // 2. 블랙리스트 등록 로직이 정상 호출되었는지 검증 (사이드 이펙트 확인)
            verify(tokenManagementService, times(1)).addAccessTokenInRedisBlackList("oldAccessToken");
            // 3. DB 조회가 발생하지 않았는지 확인
            verify(refreshTokenBackupRepository, never()).findByHashedTokenValue(anyString());
        }

        @Test
        @DisplayName("[성공 2] RefreshTokenBackup Table에서 Refresh Token 조회 후 AccessToken 재발급")
        void successSearchRefreshTokenInRefreshTokenBackupTable(){
            // given
            RefreshReqDto reqDto = new RefreshReqDto("oldAccessToken", "rawRefreshToken");
            String hashedToken = "hashedRefreshToken";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

            // User Entity Mocking
            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getRole()).thenReturn(RoleType.ROLE_USER);

            // AuthAccount Entity Mocking
            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getId()).thenReturn(1L);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getProvider()).thenReturn(Provider.GUEST);
            when(authAccount.getHashedDeviceUid()).thenReturn("hashedDeviceUid");

            // RefreshTokenBackup Entity Mocking
            RefreshTokenBackup backup = mock(RefreshTokenBackup.class);
            when(backup.getAuthAccount()).thenReturn(authAccount);
            when(backup.getExpiresAt()).thenReturn(expiresAt);

            // 새롭게 생성할 Access Token 생성
            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));

            // Stub 설정
            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(null); // Redis Miss 발생
            when(refreshTokenBackupRepository.findByHashedTokenValue(hashedToken)).thenReturn(Optional.of(backup));

            when(jwtUtil.createSubject(Provider.GUEST, "hashedDeviceUid")).thenReturn("GUEST:hashedDeviceUid");
            when(jwtUtil.createAccessToken(1L, 1L, "GUEST:hashedDeviceUid", RoleType.ROLE_USER)).thenReturn(newAccessTokenInfo);

            // when
            JwtTokenResDto resDto = authService.refreshToken(reqDto);

            // then
            // 1. resDto의 accessToken, refreshToken값에 Mock으로 넣은 데이터가 있고, recoverToken에는 값이 없는지 확인
            assertThat(resDto.accessToken()).isEqualTo("newAccessToken");
            assertThat(resDto.refreshToken()).isEqualTo("rawRefreshToken");
            assertThat(resDto.recoverToken()).isNull();

            // tokenManagementService 로직 실행 검증
            verify(tokenManagementService, times(1)).addAccessTokenInRedisBlackList("oldAccessToken");
            // Redis 복구 로직이 실행되었는지 확인 (registerToRedis 호출 여부)
            verify(tokenManagementService, times(1)).restoreHashedRefreshTokenRedisValue(hashedToken, user, authAccount, expiresAt);
        }

        @Test
        @DisplayName("[성공 3] 클라이언트가 보낸 Access Token이 null인 경우에도 예외 없이 동작")
        void successWhenAccessTokenIsNull() {
            // given
            RefreshReqDto reqDto = new RefreshReqDto(null, "rawRefreshToken"); // accessToken에 null 전달
            String hashedToken = "hashedRefreshToken";

            RedisValue redisValue = RedisValue.builder()
                    .userId(1L)
                    .authAccountId(1L)
                    .subject("GUEST:deviceUid")
                    .role(RoleType.ROLE_USER)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));

            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(redisValue);
            when(jwtUtil.createAccessToken(anyLong(), anyLong(), anyString(), any())).thenReturn(newAccessTokenInfo);

            // when
            JwtTokenResDto resDto = authService.refreshToken(reqDto);

            // then
            assertThat(resDto.accessToken()).isNotNull();
            // NullPointerException 없이 redisUtil.setTokenInBlacklist(null)이 호출되었는지 검증
            verify(tokenManagementService, times(1)).addAccessTokenInRedisBlackList(null);
        }

        @Test
        @DisplayName("[실패 1] RefreshTokenBackup Table에서 Refresh Token을 찾지 못한 경우")
        void failSearchRefreshTokenInRefreshTokenBackupTable(){
            // given
            RefreshReqDto reqDto = new RefreshReqDto("oldAccessToken", "rawRefreshToken");
            String hashedToken = "hashedRefreshToken";

            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(null); // Redis에 없음
            when(refreshTokenBackupRepository.findByHashedTokenValue(hashedToken)).thenReturn(Optional.empty()); // DB도 비었음

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.refreshToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패 2] RefreshTokenBackup Table에서 찾은 Refresh Token이 만료된 경우 - 만료된 토큰이 GUEST인 경우")
        void failGuestRefreshTokenExpired() {
            // given
            RefreshReqDto reqDto = new RefreshReqDto("oldAccessToken", "rawRefreshToken");
            String hashedToken = "hashedRefreshToken";

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getProvider()).thenReturn(Provider.GUEST); // Provider = GUEST

            RefreshTokenBackup backup = mock(RefreshTokenBackup.class);
            when(backup.getExpiresAt()).thenReturn(LocalDateTime.now().minusDays(1)); // 이미 만료된 시간 셋팅
            when(backup.getAuthAccount()).thenReturn(authAccount);

            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(null);
            when(refreshTokenBackupRepository.findByHashedTokenValue(hashedToken)).thenReturn(Optional.of(backup));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.refreshToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED_GUEST);
        }

        @Test
        @DisplayName("[실패 3] RefreshTokenBackup Table에서 찾은 Refresh Token이 만료된 경우 - 만료된 토큰이 GOOGLE인 경우")
        void failGoogleRefreshTokenExpired() {
            // given
            RefreshReqDto reqDto = new RefreshReqDto("oldAccessToken", "rawRefreshToken");
            String hashedToken = "hashedRefreshToken";

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getProvider()).thenReturn(Provider.GOOGLE); // Provider = GOOGLE

            RefreshTokenBackup backup = mock(RefreshTokenBackup.class);
            when(backup.getExpiresAt()).thenReturn(LocalDateTime.now().minusDays(1)); // 이미 만료된 시간 셋팅
            when(backup.getAuthAccount()).thenReturn(authAccount);

            when(hashingUtil.hashingValue(reqDto.refreshToken())).thenReturn(hashedToken);
            when(redisUtil.getHashedRefreshTokenValue(hashedToken)).thenReturn(null);
            when(refreshTokenBackupRepository.findByHashedTokenValue(hashedToken)).thenReturn(Optional.of(backup));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.refreshToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED_GOOGLE);
        }

    }

    @Nested
    @DisplayName("recertificationGuestToken() 메서드 테스트")
    class RecertificationGuestTokenTest {
        @Test
        @DisplayName("[성공] Guest Refresh/Recover Token 재발급 및 세션 정보 정상 갱신")
        void successRecertificationGuestRefreshToken() {
            // given
            GuestRecertificationReqDto reqDto = new GuestRecertificationReqDto("rawDeviceUid", "oldAccessToken", "rawRecoverToken");
            String hashedDeviceUid = "hashedDeviceUid";
            String hashedRecoverToken = "hashedRecoverToken";

            // User Entity 모킹
            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getRole()).thenReturn(RoleType.ROLE_USER);
            when(user.getStatus()).thenReturn(UserStatus.ACTIVE); // 삭제 대기가 아닌 정상 상태

            // AuthAccount Entity 모킹
            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getId()).thenReturn(1L);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getProvider()).thenReturn(Provider.GUEST);
            when(authAccount.getHashedDeviceUid()).thenReturn(hashedDeviceUid);
            when(authAccount.getHashedRecoverToken()).thenReturn(hashedRecoverToken); // 요청과 동일한 토큰 값으로 설정

            when(hashingUtil.hashingValue(reqDto.deviceUid())).thenReturn(hashedDeviceUid);
            when(hashingUtil.hashingValue(reqDto.recoverToken())).thenReturn(hashedRecoverToken);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST))
                    .thenReturn(Optional.of(authAccount));

            // 기존 Refresh Token 삭제 로직을 위한 모킹
            RefreshTokenBackup oldBackup = mock(RefreshTokenBackup.class);
            when(refreshTokenBackupRepository.findByAuthAccount(authAccount)).thenReturn(Optional.of(oldBackup));

            // 신규 발급 토큰 정보 모킹
            String subject = "GUEST:hashedDeviceUid";
            when(jwtUtil.createSubject(Provider.GUEST, hashedDeviceUid)).thenReturn(subject);

            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));
            TokenInfo newRefreshTokenInfo = new TokenInfo("newRefreshToken", LocalDateTime.now().plusDays(1));
            TokenInfo newRecoverTokenInfo = new TokenInfo("newRecoverToken", LocalDateTime.now().plusDays(30));

            when(jwtUtil.createAccessToken(eq(1L), eq(1L), eq(subject), eq(RoleType.ROLE_USER))).thenReturn(newAccessTokenInfo);
            when(jwtUtil.createRefreshToken(eq(subject), eq(RoleType.ROLE_USER))).thenReturn(newRefreshTokenInfo);
            when(jwtUtil.createRecoverToken(any(CustomUserDetails.class))).thenReturn(newRecoverTokenInfo);

            when(hashingUtil.hashingValue(newRecoverTokenInfo.token())).thenReturn("newHashedRecoverToken");

            // when
            JwtTokenResDto resDto = authService.recertificationGuestToken(reqDto);

            // then
            assertThat(resDto.accessToken()).isEqualTo("newAccessToken");
            assertThat(resDto.refreshToken()).isEqualTo("newRefreshToken");
            assertThat(resDto.recoverToken()).isEqualTo("newRecoverToken");

            // [검증 1] 기존 AccessToken 블랙리스트 등록 확인
            verify(tokenManagementService, times(1)).addAccessTokenInRedisBlackList("oldAccessToken");
            // [검증 2] Redis에서 기존 RefreshToken 삭제 확인
            verify(tokenManagementService, times(1)).deleteOldRefreshTokenFromRedis(oldBackup, 1L);
            // [검증 3] AuthAccount의 RecoverToken 값이 갱신되었는지 확인
            verify(authAccount, times(1)).updateHashedRecoverToken("newHashedRecoverToken");
            // [검증 4] 새로운 RefreshToken이 Redis에 잘 등록되었는지 확인
            verify(tokenManagementService, times(1)).saveRefreshTokenToTableAndRedis(newRefreshTokenInfo, user, authAccount);
        }

        @Test
        @DisplayName("[엣지 케이스] Access Token이 null인 상태로 요청이 와도 정상적으로 처리되는가")
        void successWhenAccessTokenIsNull() {
            // given
            // Access Token 자리에 null 전달
            GuestRecertificationReqDto reqDto = new GuestRecertificationReqDto("rawDeviceUid", null,"rawRecoverToken");
            String hashedDeviceUid = "hashedDeviceUid";
            String hashedRecoverToken = "hashedRecoverToken";

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getRole()).thenReturn(RoleType.ROLE_USER);
            when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getId()).thenReturn(1L);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getProvider()).thenReturn(Provider.GUEST);
            when(authAccount.getHashedDeviceUid()).thenReturn(hashedDeviceUid);
            when(authAccount.getHashedRecoverToken()).thenReturn(hashedRecoverToken);

            when(hashingUtil.hashingValue("rawDeviceUid")).thenReturn(hashedDeviceUid);
            when(hashingUtil.hashingValue("rawRecoverToken")).thenReturn(hashedRecoverToken);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST))
                    .thenReturn(Optional.of(authAccount));

            when(jwtUtil.createSubject(Provider.GUEST, hashedDeviceUid)).thenReturn("GUEST:"+hashedDeviceUid);
            when(jwtUtil.createAccessToken(anyLong(), anyLong(), anyString(), any(RoleType.class))).thenReturn(new TokenInfo("newAccessToken", LocalDateTime.now()));
            when(jwtUtil.createRefreshToken(anyString(), any(RoleType.class))).thenReturn(new TokenInfo("newRefreshToken", LocalDateTime.now()));
            when(jwtUtil.createRecoverToken(any(CustomUserDetails.class))).thenReturn(new TokenInfo("newRecoverToken", LocalDateTime.now()));

            // when
            authService.recertificationGuestToken(reqDto);

            // then
            // null이 들어가더라도 NPE 없이 블랙리스트 메서드가 호출되었는지 확인
            verify(redisUtil, never()).setTokenInBlacklist(null);
        }

        @Test
        @DisplayName("[실패 1] 해당 사용자의 AuthAccount 객체 찾기 실패")
        void failFindAuthAccount(){
            // given
            GuestRecertificationReqDto reqDto = new GuestRecertificationReqDto("rawDeviceUid", "rawRecoverToken", "oldAccessToken");
            String hashedDeviceUid = "hashedDeviceUid";

            when(hashingUtil.hashingValue("rawDeviceUid")).thenReturn(hashedDeviceUid);
            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST))
                    .thenReturn(Optional.empty()); // DB에 없음

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGuestToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패 2] Recover Token이 AuthAccount의 값과 같지 않은 경우")
        void failRecoverTokenNotEqualWithAuthAccount(){
            // given
            GuestRecertificationReqDto reqDto = new GuestRecertificationReqDto("rawDeviceUid", "oldAccessToken", "wrongRecoverToken");
            String hashedDeviceUid = "hashedDeviceUid";
            String wrongHashedRecoverToken = "wrongHashedRecoverToken";

            User user = mock(User.class);
            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getHashedRecoverToken()).thenReturn("correctHashedRecoverToken"); // DB에 저장된 정상 값

            when(hashingUtil.hashingValue("rawDeviceUid")).thenReturn(hashedDeviceUid);
            when(hashingUtil.hashingValue("wrongRecoverToken")).thenReturn(wrongHashedRecoverToken);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST))
                    .thenReturn(Optional.of(authAccount));

            // when & then
            // 1. 제대로 예외가 터지는지 확인
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGuestToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_RECOVER_TOKEN);
            // 2. 보안을 위해 사용자의 상태를 탈퇴 대기로 변경하는 withdrawUser() 로직이 실행되었는지 확인!
            verify(user, times(1)).withdrawUser(any());
        }

        @Test
        @DisplayName("[실패 3] 인증 정보를 갖고 있는 User가 삭제대기인 경우")
        void failUserWithdrawPending(){
            // given
            GuestRecertificationReqDto reqDto = new GuestRecertificationReqDto("rawDeviceUid", "oldAccessToken", "rawRecoverToken");
            String hashedDeviceUid = "hashedDeviceUid";
            String hashedRecoverToken = "hashedRecoverToken";

            User user = mock(User.class);
            when(user.getStatus()).thenReturn(UserStatus.WITHDRAW_PENDING); // 사용자가 삭제 대기 상태임

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getHashedRecoverToken()).thenReturn(hashedRecoverToken); // Recover Token은 일치하게 둠

            when(hashingUtil.hashingValue("rawDeviceUid")).thenReturn(hashedDeviceUid);
            when(hashingUtil.hashingValue("rawRecoverToken")).thenReturn(hashedRecoverToken);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST))
                    .thenReturn(Optional.of(authAccount));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGuestToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }
    }

    @Nested
    @DisplayName("recertificationGoogleToken() 메서드 테스트")
    class RecertificationGoogleTokenTest {
        @Test
        @DisplayName("[성공 1] 기존 AccessToken이 존재하는 경우 정상 재발급 및 세션 갱신")
        void successRecertificationGoogleTokenWithAccessToken() {
            // given
            GoogleRecertificationReqDto reqDto = new GoogleRecertificationReqDto("validIdToken", "oldAccessToken");
            String googleSub = "googleSub123";
            String hashedGoogleSub = "hashedGoogleSub123";
            String email = "test@gmail.com";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);
            payload.setEmail(email);

            when(googleOAuthService.verifyToken(reqDto.idToken())).thenReturn(payload);
            when(hashingUtil.hashingValue(googleSub)).thenReturn(hashedGoogleSub);

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getRole()).thenReturn(RoleType.ROLE_USER);
            when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
            when(user.getEmail()).thenReturn(email);

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getId()).thenReturn(1L);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getProvider()).thenReturn(Provider.GOOGLE);
            when(authAccount.getHashedDeviceUid()).thenReturn(hashedGoogleSub);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE))
                    .thenReturn(Optional.of(authAccount));

            // 기존 Refresh Token 삭제 로직 모킹
            RefreshTokenBackup oldBackup = mock(RefreshTokenBackup.class);
            when(refreshTokenBackupRepository.findByAuthAccount(authAccount)).thenReturn(Optional.of(oldBackup));

            // 신규 토큰 발급 모킹
            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));
            TokenInfo newRefreshTokenInfo = new TokenInfo("newRefreshToken", LocalDateTime.now().plusDays(1));

            when(jwtUtil.createAccessToken(any(CustomUserDetails.class))).thenReturn(newAccessTokenInfo);
            when(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).thenReturn(newRefreshTokenInfo);

            // when
            JwtTokenResDto resDto = authService.recertificationGoogleToken(reqDto);

            // then
            assertThat(resDto.accessToken()).isEqualTo("newAccessToken");
            assertThat(resDto.refreshToken()).isEqualTo("newRefreshToken");
            assertThat(resDto.recoverToken()).isNull(); // Google 로그인은 Recover Token이 없음
            assertThat(resDto.email()).isEqualTo(email);

            // 사이드 이펙트 검증
            verify(tokenManagementService, times(1)).addAccessTokenInRedisBlackList("oldAccessToken");
            verify(tokenManagementService, times(1)).deleteOldRefreshTokenFromRedis(oldBackup, 1L);
            verify(tokenManagementService, times(1)).saveRefreshTokenToTableAndRedis(newRefreshTokenInfo, user, authAccount);
        }

        @Test
        @DisplayName("[성공 2] AccessToken이 null이거나 빈 문자열인 경우 예외 없이 정상 재발급")
        void successRecertificationGoogleTokenWithoutAccessToken() {
            // given
            // Access Token에 null 전달
            GoogleRecertificationReqDto reqDto = new GoogleRecertificationReqDto("validIdToken", null);
            String googleSub = "googleSub123";
            String hashedGoogleSub = "hashedGoogleSub123";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);

            when(googleOAuthService.verifyToken(reqDto.idToken())).thenReturn(payload);
            when(hashingUtil.hashingValue(googleSub)).thenReturn(hashedGoogleSub);

            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getRole()).thenReturn(RoleType.ROLE_USER);
            when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getId()).thenReturn(1L);
            when(authAccount.getUser()).thenReturn(user);
            when(authAccount.getProvider()).thenReturn(Provider.GOOGLE);
            when(authAccount.getHashedDeviceUid()).thenReturn(hashedGoogleSub);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE))
                    .thenReturn(Optional.of(authAccount));

            // 기존 Refresh Token 삭제 로직 모킹
            RefreshTokenBackup oldBackup = mock(RefreshTokenBackup.class);
            when(refreshTokenBackupRepository.findByAuthAccount(authAccount)).thenReturn(Optional.of(oldBackup));

            // 신규 토큰 발급 모킹
            TokenInfo newAccessTokenInfo = new TokenInfo("newAccessToken", LocalDateTime.now().plusHours(1));
            TokenInfo newRefreshTokenInfo = new TokenInfo("newRefreshToken", LocalDateTime.now().plusDays(1));

            when(jwtUtil.createAccessToken(any(CustomUserDetails.class))).thenReturn(newAccessTokenInfo);
            when(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).thenReturn(newRefreshTokenInfo);

            // when
            authService.recertificationGoogleToken(reqDto);

            // then
            // null이 전달되었으므로 방어 로직에 의해 블랙리스트 등록 로직이 호출되지 않아야 함
            verify(tokenManagementService, never()).addAccessTokenInRedisBlackList(any());
        }

        @Test
        @DisplayName("[실패 1] 등록되지 않은 Google 사용자(DB에 AuthAccount 없음)의 접근")
        void failGoogleUserNotFound() {
            // given
            GoogleRecertificationReqDto reqDto = new GoogleRecertificationReqDto("validIdToken", "oldAccessToken");
            String googleSub = "googleSub123";
            String hashedGoogleSub = "hashedGoogleSub123";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);

            when(googleOAuthService.verifyToken(reqDto.idToken())).thenReturn(payload);
            when(hashingUtil.hashingValue(googleSub)).thenReturn(hashedGoogleSub);

            // DB에 계정이 없다고 가정
            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE))
                    .thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGoogleToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패 2] 인증 정보를 가진 User가 삭제 대기(WITHDRAW_PENDING) 상태인 경우")
        void failGoogleUserWithdrawPending() {
            // given
            GoogleRecertificationReqDto reqDto = new GoogleRecertificationReqDto("validIdToken", "oldAccessToken");
            String googleSub = "googleSub123";
            String hashedGoogleSub = "hashedGoogleSub123";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject(googleSub);

            when(googleOAuthService.verifyToken(reqDto.idToken())).thenReturn(payload);
            when(hashingUtil.hashingValue(googleSub)).thenReturn(hashedGoogleSub);

            // 삭제 대기 상태 모킹
            User user = mock(User.class);
            when(user.getStatus()).thenReturn(UserStatus.WITHDRAW_PENDING);

            AuthAccount authAccount = mock(AuthAccount.class);
            when(authAccount.getUser()).thenReturn(user);

            when(authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE))
                    .thenReturn(Optional.of(authAccount));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> {
                authService.recertificationGoogleToken(reqDto);
            });
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

    }

}
