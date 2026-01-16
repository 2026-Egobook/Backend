package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.GuestJoinReqDto;
import com.example.egobook_be.domain.auth.dto.JwtTokenDto;
import com.example.egobook_be.domain.auth.dto.TokenInfo;
import com.example.egobook_be.domain.auth.dto.UserAuthDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.security.CustomUserDetails;
import com.example.egobook_be.global.util.AccountCodeGenerator;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.UserNicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Auth 관련 비즈니스 로직을 수행하는
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final HashingUtil hashingUtil;
    private final UserNicknameGenerator userNicknameGenerator;

    /**
     * Guest로 회원가입을 수행하는 함수이다.
     * 실제로는 기기가 보낸 Device Uid 값을 DB에 등록한 후, 바로 3종 토큰(Access, Refresh, Recover)들을 발급해주는 과정이다.
     * - Refresh Token은 백업 테이블에 저장된다.
     * - Recover Token은 AuthAccount 테이블에 영구 저장된다.
     * - **특이사항**:
     *   (1) Recover Token은 이 함수와 "Recover Token으로 Refresh Token을 새로 발급하는 함수"에서만 재발급된다.
     *   (2) DeviceUid, Refresh Token, Recover Token은 HmacSHA256 알고리즘으로 해싱된다.
     * @param reqDto : GuestJoinReqDto
     * @return JwtTokenDto : Access, Refresh, Recover Token이 담겨있다.
     */
    @Transactional
    public JwtTokenDto registerGuest(GuestJoinReqDto reqDto){
        /**
         * 1. 중복 가입 방지
         * 이미 Guest로 등록된 기기인지 확인한다.
         */
        if(authAccountRepository.existsByHashedDeviceUidAndProvider(reqDto.deviceUid(), Provider.GUEST)){
            throw new CustomException(AuthErrorCode.ALREADY_REGISTERED_DEVICE);
        }

        /**
         * 2. 신규 User Entity 생성
         * 새롭게 가입하는 사용자의 Entity를 생성한다.
         * AuthAccount -> User Entity의 연관관계 설정을 위해, UserRepository로 먼저 save한다.
         *  (1) accountCode: 랜덤으로 생성된 공개 고유 ID 지정 (중복 여부 확인)
         *  (2) role : Builder.Default로 기본값 ROLE_USER 자동 지정
         *  (3) nickname : AccountCodeGenerator에서 자동으로 닉네임 생성 (DB에서 중복 체크도 함)
         *  (4) status: Builder.Default로 기본값 UserStatus.NEW 지정
         *  (5) email: Guest에서는 생성할 때 email을 지정 안한다. 나중에 사용자가 구글로 로그인하였을 때 채워진다.
         *  (6) streakCount: 연속 출석 수. 생성할 때 초기화되는 0으로 그대로 둔다.
         *  (7) lastLoginAt: 처음 회원가입 하는 이 시점으로 설정
         *  (8) level: 처음 시작 레벨 1로 기본 설정되어있음
         *  (9) purgeAt, deletedAt: 설정 안함
         *  (10) dailyPraise: AI 칭찬서 수신 여부. 기본값인 true로 설정되어있음
         *  (11) weeklyReportStyle: 주간 AI 상담서 스타일. 기본값인 SOFT로 설정되어있음
         *  (12) ink: 잉크 개수. 기본값인 0으로 생성됨
         */
        String accountCode = null;
        do{
            accountCode = AccountCodeGenerator.generateAccountCode();
        }
        while(userRepository.existsByAccountCode(accountCode));
        User user = User.builder()
                .accountCode(accountCode)
                .nickname(userNicknameGenerator.generateUniqueNickname())
                .lastLoginAt(LocalDateTime.now())
                .build();
        userRepository.save(user); // AuthAccount -> User Entity의 연관관계 설정을 위해, UserRepository로 먼저 save한다.

        /**
         * 3. AuthAccount 엔티티 생성 (Guest Provider)
         * - recoverToken은 token을 발급받은 뒤 AuthAccount Entity의 updateRecoverToken()을 활용하여 설정합니다.
         *  (1) provider: Guest로 설정
         *  (2) deviceId: reqDto에 있는 deviceUid 설정 (HmacSHA256으로 암호화 하여 저장. 단방향 해싱)
         *  (3) user: 위에서 생성한 newUser 설정
         *  (4) recoverToken: 아래 다른 로직에서 AuthAccount Entity의 updateRecoverToken()을 활용하여 설정합니다.
         */
        String hashedDeviceUid = hashingUtil.hashingValue(reqDto.deviceUid()); // 단방향 해싱
        AuthAccount authAccount = AuthAccount.builder()
                .provider(Provider.GUEST)
                .hashedDeviceUid(hashedDeviceUid)
                .user(user)
                .build();
        authAccountRepository.save(authAccount); // AuthAccount -> User Entity의 연관관계 설정을 위해, authRepository로 먼저 save한다.

        /**
         * 4. 토큰 발급을 위한 UserDetails 생성
         *  (1) 토큰 발급 시 필요한 사용자 인증 정보를 담은 UserAuthDto를 생성한다.
         *  (2) 생성한 UserAuthDto를 기반으로 CustomUserDetails를 생성한다.
         */
        UserAuthDto userAuthDto = UserAuthDto.builder()
                .userId(user.getId())
                .authAccountId(authAccount.getId())
                .provider(authAccount.getProvider())
                .accountCode(user.getAccountCode())
                .deviceUid(authAccount.getHashedDeviceUid()) // authAccount에 들어있는 deviceUid는 해싱된 상태이다.
                .role(user.getRole())
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userAuthDto);

        /**
         * 5. Access, Refresh, Recover Token 생성
         */
        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);
        TokenInfo recoverTokenInfo = jwtUtil.createRecoverToken(userDetails);

        /**
         * 6. Recover Token을 AuthAccount에 저장 (영구 보관용)
         * **주의**: 이때, recoverToken 값은 HmacSHA256으로 해싱하여 저장한다. (단방향 해싱)
         */
        String hashedRecoverToken = hashingUtil.hashingValue(recoverTokenInfo.token());
        authAccount.updateRecoverToken(hashedRecoverToken);

        /**
         * 7. Refresh Token을 RefreshTokenBackup Table에 추가(Update)
         */
        saveRefreshTokenBackup(authAccount, hashedRecoverToken, recoverTokenInfo.expiresAt());

        /**
         * 8. 클라이언트에게 토큰을 반환
         * recoverToken은 회원가입, refreshToken 재발급 시에만 발급된다.
         */
        return JwtTokenDto.builder()
                .accessToken(accessTokenInfo.token())
                .refreshToken(refreshTokenInfo.token())
                .recoverToken(recoverTokenInfo.token())
                .build();
    }

    /**
     * registerGuest - 6. Refresh Token을 RefreshTokenBackup Table에 추가(Update)
     * 새로 생성한 RefreshToken을 RefreshTokenBackup 테이블에 업데이트 하는 함수이다.
     * - 기존에 해당 authAccount PK 행이 존재하면 업데이트, 없다면 새로 추가한다.
     * [ 업데이트 로직 ]
     *  (1) authAccount.deviceUid -> refreshTokenBackup.deviceUid (authAccount 테이블이 deviceUid를 관리하는 책임자이다.)
     *  (2) TokenInfo.token -> refreshTokenBackup.tokenValue
     *  (3) TokenInfo.expiresAt -> refreshTokenBackup.expiresAt
     *
     * [ 신규 추가 로직 ]
     *  (1) RefreshTokenBackup 새로 생성하여 authAccount.updateRefreshTokenBackup(...)으로 연결
     *  -> 영속성 컨텍스트의 Dirty Checking으로 트랜잭션 종료 시 Update됨
     * @param authAccount : 새로 생성한 AuthAccount 객체
     * @param hashedRefreshToken : 새로 발급한 refreshToken을 해싱한 결과값
     * @param expiresAt : refreshToken의 만료 시간
     */
    private void saveRefreshTokenBackup(AuthAccount authAccount, String hashedRefreshToken, LocalDateTime expiresAt){
        RefreshTokenBackup backup = null;
        /**
         * 1. RefreshTokenBackup Table에 해당 authAccount PK가 존재하는 경우
         * 기존에 해당 테이블에 authAccount Pk가 존재한다면, 기존 Row를 Update한다.
         */
        if(refreshTokenBackupRepository.existsByAuthAccount(authAccount)){
            backup = refreshTokenBackupRepository.findByAuthAccount(authAccount).orElseThrow(() -> new CustomException(AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND_IN_REFRESH_TOKEN_BACKUP));
            backup.updateBackupInfo(authAccount.getHashedDeviceUid(), hashedRefreshToken, expiresAt); // RefreshTokenBackup 테이블의 내용을 업데이트한다.
        }
        /**
         * 2. RefreshTokenBackup Table에 해당 authAccount PK가 존재하는 경우
         * 새로운 RefreshTokenBackup 객체를 생성하여 authAccount에 연관관계를 추가한다.
         */
        else{
            backup = RefreshTokenBackup.builder()
                    .authAccount(authAccount)
                    .hashedDeviceUid(authAccount.getHashedDeviceUid()) // 이미 암호화된 deviceUid이므로, 한번 더 해싱하면 안된다.
                    .hashedTokenValue(hashedRefreshToken) // 암호화된 refreshToken을 저장한다.
                    .expiresAt(expiresAt)
                    .build();
            authAccount.updateRefreshTokenBackup(backup);
        }
    }
}
