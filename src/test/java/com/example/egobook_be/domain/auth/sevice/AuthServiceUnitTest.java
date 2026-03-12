package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.security.CustomUserDetails;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.UserNicknameGenerator;
import com.example.egobook_be.global.util.module.RedisValue;
import com.example.egobook_be.global.util.module.TokenInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {
    @InjectMocks
    private AuthService authService;

    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private AbilityRepository abilityRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private HashingUtil hashingUtil;
    @Mock private UserNicknameGenerator userNicknameGenerator;
    @Mock private RedisUtil redisUtil;
    @Mock private TermRepository termRepository;
    @Mock private UserTermRepository userTermRepository;
    @Mock private MissionRepository missionRepository;

    @Value("${app.data.purge-duration-in-ms}")
    private Long purgeDurationInMs;

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

            /*
             * 3. 신규 User Entity 생성 로직으로 전환 (createUser())
             * - userRepository.existsByAccountCode(어떤 문자열이던) : false 반환
             * - userNicknameGenerator.generateUniqueNickname() :  Ego1234 반환
             */
            given(userRepository.existsByAccountCode(anyString())).willReturn(false);
            given(userNicknameGenerator.generateUniqueNickname()).willReturn("Ego1234");

            // userRepository가 반환할 mockUser
            User mockUser = User.builder()
                    .id(1L)
                    .accountCode(accountCode)
                    .nickname(userNicknameGenerator.generateUniqueNickname())
                    .lastLoginAt(LocalDateTime.now())
                    .email(email)
                    .build();
            // save에 User 클래스만 들어가면 미리 설정해둔 mockUser를 반환하도록 Stub 설정
            given(userRepository.save(any(User.class))).willReturn(mockUser);

            /*
             * 4. allocateUser() 내부 메서드 - createDefaultUserItems()
             * - defaultItem.getFullUrl() 부분에서 에러 반환하지 않도록 defaultItems에 필요한 값이 들어간 Item 생성 및 주입
             */
            given(itemRepository.findAllByName("Default.png")).willReturn(List.of(Item.builder().path("tmpPath").name("skin").build()));
            given(userItemRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
            // -> InvocationOnMock::getArguments = userItemRepository.saveAll()의 함수의 파라미터로 들어간 인자들 중 첫번째 인자의 값 추출

            // 5. allocateUser() 내부 메서드 - createDefaultUserTerms()
            given(termRepository.findAll()).willReturn(List.of(Term.builder().build()));

            /*
             * 6. createAuthAccount()
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
             * 7. processIssueTokens()
             * - jwtUtil.createAccessToken(), createRefreshToken() Mock, Stub 설정
             * - processRefreshTokenSaving - hashingUtil.hashingValue() Mock, Stub 설정
             * - processRefreshTokenSaving - jwtUtil.createSubject() Mock, Stub 설정
             * - updateRefreshTokenBackupTable - refreshTokenBackupRepository.existsByAuthAccount() Stub 설정
             */
            TokenInfo mockAccessTokenInfo = TokenInfo.builder().token("access-token-value").expiresAt(LocalDateTime.now().plusHours(1)).build();
            TokenInfo mockRefreshTokenInfo = TokenInfo.builder().token("refresh-token-value").expiresAt(LocalDateTime.now().plusHours(24)).build();
            String hashedRefreshToken = "hashed-refresh-token-value";
            String subject = Provider.GOOGLE + ":" + hashedGoogleSub;

            given(jwtUtil.createAccessToken(any(CustomUserDetails.class))).willReturn(mockAccessTokenInfo);
            given(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).willReturn(mockRefreshTokenInfo);
            given(jwtUtil.createSubject(Provider.GOOGLE, hashedGoogleSub)).willReturn(subject);
            given(hashingUtil.hashingValue("refresh-token-value")).willReturn(hashedRefreshToken);
            given(refreshTokenBackupRepository.existsByAuthAccount(mockAuthAccount)).willReturn(false);

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
            verify(userRepository, times(1)).save(any(User.class));
            verify(authAccountRepository, times(1)).save(any(AuthAccount.class));
            verify(redisUtil, times(1)).setHashedRefreshTokenValue(anyString(), any(RedisValue.class), anyLong());
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
            String hashedRefreshToken = "hashed-refresh-token-value";
            String subject = Provider.GOOGLE + ":" + hashedGoogleSub;

            given(jwtUtil.createAccessToken(any(CustomUserDetails.class))).willReturn(mockAccessTokenInfo);
            given(jwtUtil.createRefreshToken(any(CustomUserDetails.class))).willReturn(mockRefreshTokenInfo);
            given(jwtUtil.createSubject(Provider.GOOGLE, hashedGoogleSub)).willReturn(subject);
            given(hashingUtil.hashingValue("refresh-token-value")).willReturn(hashedRefreshToken);
            given(refreshTokenBackupRepository.existsByAuthAccount(mockAuthAccount)).willReturn(false);

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
            verify(redisUtil, times(1)).setHashedRefreshTokenValue(anyString(), any(RedisValue.class), anyLong()); // Hashed Refresh Token Redis에 등록하는 행위
            // 기존 User의 데이터 복구이므로, 아래의 save 로직은 절대 타지 말아야함
            verify(userRepository, never()).save(any(User.class));
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
            verify(userRepository, never()).save(any(User.class));
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
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).createAccessToken(any());
        }
    }




}
