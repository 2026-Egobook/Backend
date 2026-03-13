package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.GoogleJoinReqDto;
import com.example.egobook_be.domain.auth.dto.req.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // application-test.yml
public class AuthServiceIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private GoogleOAuthService googleOAuthService;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private RefreshTokenBackupRepository refreshTokenBackupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private HashingUtil hashingUtil;
    @Autowired private UserNicknameGenerator userNicknameGenerator;
    @Autowired private RedisUtil redisUtil;
    @Autowired private TermRepository termRepository;
    @Autowired private AbilityRepository abilityRepository;
    @Autowired private UserTermRepository userTermRepository;
    @Autowired private MissionRepository missionRepository;


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



    }
    
    
}
