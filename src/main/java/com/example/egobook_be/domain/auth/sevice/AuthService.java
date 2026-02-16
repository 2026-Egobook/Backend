package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.*;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.entity.UserTerm;
import com.example.egobook_be.domain.terms.enums.TermErrorCode;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.global.util.*;
import com.example.egobook_be.global.util.module.TokenInfo;
import com.example.egobook_be.global.util.module.UserAuthDto;
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
import com.example.egobook_be.global.util.module.RedisValue;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Auth 관련 비즈니스 로직을 수행하는
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final GoogleOAuthService googleOAuthService;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final AbilityRepository abilityRepository;
    private final JwtUtil jwtUtil;
    private final HashingUtil hashingUtil;
    private final UserNicknameGenerator userNicknameGenerator;
    private final RedisUtil redisUtil;
    private final TermRepository termRepository;
    private final UserTermRepository userTermRepository;
    private final MissionRepository missionRepository;

    @Value("${app.data.purge-duration-in-ms}")
    private Long purgeDurationInMs;

    // ==================================================================
    // [Public] Google 비즈니스 메서드
    // ==================================================================

    /**
     * Google로 처음 회원가입을 수행하는 함수이다.
     * 클라이언트가 보낸 Google Id가 유효한지, Google sub를 통해 AuthAccount 테이블에 겹치는 사용자는 없는지 검증한다.
     * 해당 토큰이 유효하다면, 해당 사용자의 정보를 DB에 저장하고 Access, Refresh Token을 발급해서 반환한다.
     * **주의사항**
     *  1. Google 계정은 Recover Token을 발급하지 않는다.
     *  2. Google Sub값을 해싱하여, AuthAccount Table의 HashedDeviceUid 컬럼에 저장한다.
     */
    @Transactional
    public JwtTokenResDto registerGoogle(GoogleJoinReqDto reqDto){
        /*
         * 1. Google ID Token 검증 및 정보 추출
         * - 유효하지 않은 토큰이면 GoogleOAuthService에서 예외가 발생한다.
         * - payload에서 sub(고유 식별자)와 email을 추출한다.
         * - 해당 google sub은 email이 바뀌어도 변하지 않는다.
         */
        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(reqDto.idToken());
        String googleSub = payload.getSubject();
        String email = payload.getEmail();

        /*
         * 2. 중복 가입 방지
         * - Google Sub를 해싱하여 기존에 가입된 계정이 있는지 확인한다.
         * - Provider.GOOGLE과 조합하여 체크한다.
         */
        String hashedGoogleSub = hashingUtil.hashingValue(googleSub);
        AuthAccount authAccount = authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE).orElse(null);
        if(authAccount != null){
            // (1) 기존에 가입된 계정이라면, User를 찾아서 삭제 대기중인지 확인한다
            User user = authAccount.getUser();

            // (2) 만약 삭제 대기 중인 경우라면 해당 계정의 상태를 ACTIVE로 변경하고, 토큰을 새로 발급한 뒤 Refresh Token Backup 테이블 & Redis 갱신 후 결과 반환
            if(user.getStatus().equals(UserStatus.WITHDRAW_PENDING)){
                log.info("[Google Join] 탈퇴 대기 중인 구글 사용자가 재 회원가입을 시도하였습니다.");
                user.cancelWithDrawUser(); // 사용자 상태 복구
                return processIssueTokens(user, authAccount, email);
            }
            // (3) 만약 사용자가 삭제 상태면 예외 throw
            if(user.getStatus().equals(UserStatus.WITHDRAW)){
                log.info("[Google Join] 탈퇴된 구글 사용자가 재 회원가입을 시도하였습니다.");
                throw new CustomException(AuthErrorCode.GOOGLE_JOIN_FAIL_USER_WITHDRAWN);
            }

            // (4) 만약 사용자가 삭제 대기중도, 삭제 상태도 아니라면 이미 존재하는 사용자이므로, 중복 회원가입을 막기 위해 "중복된 사용자입니다" 응답 반환
            throw new CustomException(AuthErrorCode.ALREADY_REGISTERED_USER);
        }

        /*
         * 3. 신규 User Entity 생성 (공통 메서드 활용) (+ 처음 사용자가 회원가입했을 때 받아야할 것들 할당)
         * - 닉네임: createUser 내부에서 자동 생성된다. (만약 reqDto의 nickname을 쓰고 싶다면 createUser 수정 필요)
         * - 이메일: Google Payload에서 추출한 이메일을 저장한다.
         */
        User user = createUser(email);
        allocateUser(user);

        /*
         * 4. AuthAccount 엔티티 생성 (Google Provider)
         * - hashedDeviceUid 자리에 hashedGoogleSub를 저장한다.
         * - recoverToken은 createAuthAccount 내부에서 초기값(null)으로 설정된다.
         */
        authAccount = createAuthAccount(user, Provider.GOOGLE, hashedGoogleSub);

        // 5. Token을 발급 및 환경 세팅 수행
        return processIssueTokens(user, authAccount, email);
    }

    /** Token들을 발급하는 과정을 담은 함수 */
    private JwtTokenResDto processIssueTokens(User user, AuthAccount authAccount, String email){
        // 1. 토큰 발급을 위한 UserDetails 생성
        CustomUserDetails userDetails = buildCustomUserDetails(user, authAccount);

        /*
         * 2. Access, Refresh Token 생성
         * - **주의**: Google은 Recover Token을 생성하지 않는다.
         */
        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);

        // 3. Refresh Token을 Table, Redis에 저장하는 Process 수행
        processRefreshTokenSaving(user, authAccount, refreshTokenInfo);

        /*
         * 4. 클라이언트에게 토큰 반환
         * - Google 로그인이므로 recoverToken은 null을 반환한다.
         */
        return buildJwtTokenResDto(accessTokenInfo.token(), refreshTokenInfo.token(), null, email);
    }


    // ==================================================================
    // [Public] Guest 비즈니스 메서드
    // ==================================================================
    /**
     * Guest로 회원가입을 수행하는 함수이다.
     * 실제로는 기기가 보낸 Device Uid 값을 DB에 등록한 후, 바로 3종 토큰(Access, Refresh, Recover)들을 발급해주는 과정이다.
     * - Refresh Token은 백업 테이블에 저장된다.
     * - Recover Token은 AuthAccount 테이블에 영구 저장된다.
     * - **특이사항**:
     *   (1) Recover Token은 이 함수와 "Recover Token으로 Refresh Token을 새로 발급하는 함수"에서만 재발급된다.
     *   (2) DeviceUid, Refresh Token, Recover Token은 HmacSHA256 알고리즘으로 해싱된다.
     * @param reqDto : GuestJoinReqDto
     * @return JwtTokenResDto : Access, Refresh, Recover Token이 담겨있다.
     */
    @Transactional
    public JwtTokenResDto registerGuest(GuestJoinReqDto reqDto){
        /*
         * 1. 중복 가입 방지
         * 이미 Guest로 등록된 기기인지 확인한다.
         */
        String hashedDeviceUid = hashingUtil.hashingValue(reqDto.deviceUid()); // 단방향 해싱
        if(authAccountRepository.existsByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)){
            throw new CustomException(AuthErrorCode.ALREADY_REGISTERED_USER);
        }

        // 2. 신규 User Entity 생성 (+ 처음 사용자가 회원가입했을 때 받아야할 것들 할당)
        User user = createUser(null);
        allocateUser(user);

        // 3. AuthAccount 엔티티 생성 (Guest Provider)
        AuthAccount authAccount = createAuthAccount(user, Provider.GUEST, hashedDeviceUid);

        /*
         * 4. 토큰 발급을 위한 UserDetails 생성
         *  (1) 토큰 발급 시 필요한 사용자 인증 정보를 담은 UserAuthDto를 생성한다.
         *  (2) 생성한 UserAuthDto를 기반으로 CustomUserDetails를 생성한다.
         */
        CustomUserDetails userDetails = buildCustomUserDetails(user, authAccount);

        /*
         * 5. Access, Refresh, Recover Token 생성
         */
        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);
        TokenInfo recoverTokenInfo = jwtUtil.createRecoverToken(userDetails);

        /*
         * 6. Recover Token을 AuthAccount에 저장 (영구 보관용)
         * **주의**: 이때, recoverToken 값은 HmacSHA256으로 해싱하여 저장한다. (단방향 해싱)
         */
        authAccount.updateHashedRecoverToken(hashingUtil.hashingValue(recoverTokenInfo.token()));

        // 7. 모든 토큰들을 발급한 뒤, Refresh Token을 Table, Redis에 저장하는 Process를 수행
        processRefreshTokenSaving(user, authAccount, refreshTokenInfo);

        /*
         * 10. 클라이언트에게 토큰을 반환
         * recoverToken은 회원가입, refreshToken 재발급 시에만 발급된다.
         */
        return buildJwtTokenResDto(accessTokenInfo.token(), refreshTokenInfo.token(), recoverTokenInfo.token(), null);
    }


    /**
     * Refresh Token을 이용해 Access Token을 재발급하는 함수 (FallBack 전략 적용)
     * 1. Hashed Refresh Token으로 Redis 조회 시도 -> 조회 성공 시, value에 있는 값들을 이용하여 Access Token, Refresh Token 즉시 재발급 (RTR 적용)
     * 2. Redis에서 해당 정보 조회 실패 시 -> Refresh Token Backup 테이블 조회
     * 3. DB 조회 성공 시 -> Redis에 데이터 복구 후 토큰 재발급
     * 4. DB 조회 실패 시 -> Recover Token으로 Refresh Token 재발급 시도하라는 에러 클라이언트에게 반환
     * @param reqDto : RefreshReqDto
     * @return JwtTokenResDto
     */
    @Transactional
    public JwtTokenResDto refreshToken(RefreshReqDto reqDto){
        // 1. 전달받은 Refresh Token Hashing
        String hashedRefreshToken = hashingUtil.hashingValue(reqDto.refreshToken());
        
        /*
         * 2. Redis에서 조회 시도
         * - 조회를 성공한 경우, RedisValue Record Class에 담겨있는 값으로 Access Token을 재발급한다.
         * - 재발급 시 Recover Token은 안준다.
         */
        RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken);
        if(redisValue != null){
            // 기존 AccessToken Redis 블랙리스트에 추가
            addAccessTokenInRedisBlackList(reqDto.accessToken());
            TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(redisValue.userId(), redisValue.authAccountId(), redisValue.subject(), redisValue.role());
            return buildJwtTokenResDto(newAccessTokenInfo.token(), reqDto.refreshToken(), null, null);
        }

        /*
         * 3. Redis에서 조회를 실패했을 경우, Refresh Token Backup 테이블 조회 (Fallback)
         * - Redis에서 해당 hashedRefreshToken을 못찾았을 경우, DB에서 확인한다.
         */
        log.warn("[Class:AuthService]: Redis에서 해당 HashedRefreshToken을 찾을 수 없습니다. RefreshTokenBackup Table을 조회합니다.");
        RefreshTokenBackup backup = refreshTokenBackupRepository.findByHashedTokenValue(hashedRefreshToken)
                .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        /*
         * 4. DB에서 가져온 데이터로 해당 Refresh Token의 만료 여부 검증
         * - DB에서도 만료되었다면, 진짜 해당 Refresh Token이 만료된 것이다.
         *  [ Provider 에러 분기 처리 ]
         *  (1) Guest인 경우: Recover Token을 이용하여 Refresh Token을 재발급 받으라는 에러 메시지 발송
         *  (2) Google인 경우: Google 로그인을 다시 시도해서
         */
        if(backup.getExpiresAt().isBefore(LocalDateTime.now())){
            Provider provider = backup.getAuthAccount().getProvider();
            // 만료된 토큰의 주인이 GUEST인 경우
            if(provider == Provider.GUEST){
                throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED_GUEST);
            }
            // 만료된 토큰의 주인이 GOOGLE인 경우
            else {
                throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED_GOOGLE);
            }
        }
        /*
         * 5. DB와 비교하였을 때 모든 조건을 통과한 정상적인 Refresh Token인 경우, Access Token을 재발급한다.
         * - Redis에 데이터를 복구한다.
         * - Redis에 복구할 데이터 : RedisValue
         */
        AuthAccount authAccount = backup.getAuthAccount(); // 연결된 AuthAccount 객체 가져오기
        User user = authAccount.getUser(); // 연결된 User 객체 가져오기
        String subject = jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid());
        RedisValue restoreRedisValue = buildRedisValue(user.getId(), authAccount.getId(), subject, user.getRole(), backup.getExpiresAt()); // RedisValue 생성
        registerToRedis(hashedRefreshToken, restoreRedisValue, backup.getExpiresAt()); // Redis에 해당 데이터들 복구

        // 6. 기존 Access Token Redis의 BlackList에 추가
        addAccessTokenInRedisBlackList(reqDto.accessToken());

        // 7. Access Token 재생성 후 Access, Refresh Token 반환
        TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(user.getId(), authAccount.getId(), subject, user.getRole());
        return buildJwtTokenResDto(newAccessTokenInfo.token(), reqDto.refreshToken(), null, null);
    }

    /** HttpServletRequest에 들어있는 AccessToken 추출 및 블랙리스트 등록 */
    private void addAccessTokenInRedisBlackList(String accessToken){
            // 만료되지 않은 토큰이라도 강제로 블랙리스트 처리
            redisUtil.setTokenInBlacklist(accessToken);
            log.info("🪪 재발급 요청에 사용된 기존 Access Token을 블랙리스트에 등록했습니다.");
    }

    private void deleteOldRefreshTokenFromRedis(AuthAccount authAccount) {
        // DB에 백업된 정보가 있는지 확인
        refreshTokenBackupRepository.findByAuthAccount(authAccount).ifPresent(backup -> {
            String oldHashedToken = backup.getHashedTokenValue();

            // RedisUtil을 통해 삭제 (존재하면 삭제, 없으면 무시됨)
            if (oldHashedToken != null) {
                redisUtil.deleteHashedRefreshToken(oldHashedToken);
                log.info("🔄 [Rotation] 기존 Refresh Token을 Redis에서 삭제했습니다. User: {}", authAccount.getId());
            }
        });
    }

    /**
     * [ Guest RefreshToken 재발급 ]
     * - 사용자의 상태가 "삭제 대기" 상태라면 사용자 재인증 금지
     * - Refresh Token이 만료되었을 때, 기기에 영구 저장된 Recover Token을 검증하여 세션을 복구하는 함수
     */
    @Transactional
    public JwtTokenResDto recertificationGuestToken(GuestRecertificationReqDto reqDto){
        // 1. 전달받은 Device Uid, Recover Token Hashing
        String hashedDeviceUid = hashingUtil.hashingValue(reqDto.deviceUid());
        String hashedRecoverToken = hashingUtil.hashingValue(reqDto.recoverToken());

        // 2. AuthAccount에서 Provider, HashedDeviceUid로 AuthAccount 객체 찾기
        AuthAccount authAccount = findAuthAccountByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST);

        /*
         * 3. Hashed Recover Token 일치 여부를 검증한다.
         * - DB에 저장된 Hashed Recover Token과 요청받은 토큰이 다르면 유효하지 않은 접근으로 간주한다.
         * - 이 경우, 사용자의 상태를 "삭제 대기(DELETED_PENDING)"로 만들며, 클라이언트는 로컬 데이터를 모두 파기하고 신규 가입 절차를 밟아야 한다.
         */
        if(!authAccount.getHashedRecoverToken().equals(hashedRecoverToken)){
            log.warn("[Class:AuthService]: HashedDeviceUid: {}, 옳지 않은 Recover Token로 Refresh Token 복구 시도를 하였습니다..", hashedDeviceUid);
            authAccount.getUser().withdrawUser(purgeDurationInMs); // 사용자 삭제(실제로는 삭제 예정으로 상태 변경)
            throw new CustomException(AuthErrorCode.INVALID_RECOVER_TOKEN);
        }

        /*
         * 4. 해당 인증 정보를 갖고 있는 User의 Status가 삭제 대기 상태인지 확인한다.
         * - findAuthAccountByHashedDeviceUidAndProvider에서 User도 Fetch Join으로 가져와 같은 영속성 컨텍스트에 있으므로, N+1이 발생하지 않는다.
         */
        User user = authAccount.getUser();
        if(user.getStatus().equals(UserStatus.WITHDRAW_PENDING)){
            throw new CustomException(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

        /*
         * 5. Redis 상태 업데이트
         * (1) 기존에 사용되던 AccessToken 블랙리스트에 등록
         * (2) 기존에 Refresh Token Backup 테이블에 있던 Refresh Token Redis에서 삭제 (아직 만료되지 않은 상태에서 복구 로직이 실행될 수도 있으므로)
         */
        addAccessTokenInRedisBlackList(reqDto.accessToken());
        deleteOldRefreshTokenFromRedis(authAccount);

        /*
         * 6. 검증 통과 시, 새로운 Access, Refresh, Recover Token 생성
         * - User 정보를 로드하여 토큰 생성에 필요한 Subject, Role 획득
         * - recoverTokenInfo는 CustomUserDetails 객체가 있어야 하므로 생성
         * - AuthAccount의 hashedRecoverToken 업데이트
         */
        CustomUserDetails userDetails = buildCustomUserDetails(user, authAccount);
        String subject = jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid()); // 토큰에 넣을 subject 생성
        TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(user.getId(), authAccount.getId(), subject, user.getRole());
        TokenInfo newRefreshTokenInfo = jwtUtil.createRefreshToken(subject, user.getRole());
        TokenInfo newRecoverTokenInfo = jwtUtil.createRecoverToken(userDetails);
        authAccount.updateHashedRecoverToken(hashingUtil.hashingValue(newRecoverTokenInfo.token())); // authAccount Table의 HashedRecoverToken값 최신화

        /*
         * 7. Refresh Token 백업 테이블 업데이트
         * - 기존에 만료된 Refresh Token 정보를 새로운 토큰 정보로 덮어씌운다.
         */
        String newHashedRefreshToken = hashingUtil.hashingValue(newRefreshTokenInfo.token());
        updateRefreshTokenBackupTable(
                authAccount,
                newHashedRefreshToken,
                newRefreshTokenInfo.expiresAt());

        /*
         * 8. Redis 업데이트
         * - 새로운 Refresh Token을 Redis에 등록하여 바로 사용 가능하도록 처리
         */
        RedisValue newRedisValue = buildRedisValue(user.getId(), authAccount.getId(), subject, user.getRole(), newRefreshTokenInfo.expiresAt());
        registerToRedis(newHashedRefreshToken, newRedisValue, newRefreshTokenInfo.expiresAt());

        // 9. 결과 반환
        return buildJwtTokenResDto(newAccessTokenInfo.token(), newRefreshTokenInfo.token(), newRecoverTokenInfo.token(), null);
    }

    /**
     * Google RefreshToken 재발급
     * Refresh Token이 만료되었을 때, 프론트엔드가 Google Silent Login 후 받은 ID Token으로 호출하는 API
     * - 회원가입(Register)과 달리, 이미 존재하는 계정이어야만 성공한다.
     * - Recover Token은 여전히 null이다.
     */
    @Transactional
    public JwtTokenResDto recertificationGoogleToken(GoogleRecertificationReqDto reqDto){
        // 1. 구글 토큰 검증
        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(reqDto.idToken());
        String hashedGoogleSub = hashingUtil.hashingValue(payload.getSubject());

        /*
         * 2. 기존 계정 존재 여부 확인 (없으면 에러 -> 재인증 API이므로 가입되어 있어야 함)
         * - Google Id Token에 들어있는 사용자 고유 번호(sub) & Provider AuthAccount Table에 이미 존재하는지 검사한다.
         */
        AuthAccount authAccount = authAccountRepository.findByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        /*
         * 3. 해당 인증 정보를 갖고 있는 User의 Status가 삭제 대기 상태인지 확인한다.
         * - findAuthAccountByHashedDeviceUidAndProvider에서 User도 Fetch Join으로 가져와 같은 영속성 컨텍스트에 있으므로, N+1이 발생하지 않는다.
         */
        User user = authAccount.getUser();
        if(user.getStatus().equals(UserStatus.WITHDRAW_PENDING)){
            throw new CustomException(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

        /*
         * 4. Redis 상태 업데이트
         * (1) 기존에 사용되던 AccessToken 블랙리스트에 등록 (Access Token이 Request Dto에 있는 경우에만)
         * (2) 기존에 Refresh Token Backup 테이블에 있던 Refresh Token Redis에서 삭제 (아직 만료되지 않은 상태에서 복구 로직이 실행될 수도 있으므로)
         */
        if (reqDto.accessToken() != null && !reqDto.accessToken().isBlank()) {
            addAccessTokenInRedisBlackList(reqDto.accessToken());
        }
        deleteOldRefreshTokenFromRedis(authAccount);

        // 5. 토큰 재발급 (Access + Refresh)
        CustomUserDetails userDetails = buildCustomUserDetails(user, authAccount);
        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);

        // 5. Refresh Token 저장 (DB + Redis)
        processRefreshTokenSaving(user, authAccount, refreshTokenInfo);

        // 6. 반환
        return buildJwtTokenResDto(accessTokenInfo.token(), refreshTokenInfo.token(), null, user.getEmail());
    }

    /**
     * Guest 계정을 Google 계정으로 전환(연동)하는 함수
     * - 현재 로그인한 Guest 유저 정보를 유지하면서, 인증 수단만 Google로 교체한다.
     * - 기존 Guest 관련 데이터(AuthAccount, RefreshTokenBackup Table의 데이터)는 삭제된다.
     * * @param userId : 현재 로그인된 Guest 유저의 ID (SecurityContext에서 추출)
     * @param reqDto : Google ID Token
     */
    @Transactional
    public JwtTokenResDto linkGoogleAccount(Long userId, GoogleJoinReqDto reqDto) {
        // 1. Google ID Token 검증
        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(reqDto.idToken());
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String hashedGoogleSub = hashingUtil.hashingValue(googleSub);

        // 2. 이미 해당 구글 계정으로 가입된 유저가 있는지 확인 (중복 방지)
        if (authAccountRepository.existsByHashedDeviceUidAndProvider(hashedGoogleSub, Provider.GOOGLE)) {
            throw new CustomException(AuthErrorCode.ALREADY_REGISTERED_USER);
        }

        // 3. 현재 유저의 Guest AuthAccount 조회
        AuthAccount guestAccount = authAccountRepository.findByUserIdAndProvider(userId, Provider.GUEST)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND)); // Guest 계정이 없는 경우
        User user = guestAccount.getUser();

        // 4. 삭제할 기존 Guest Refresh Token 정보 확보 (Redis 삭제를 위해 Hashed Token 값이 필요함)
        String oldHashedRefreshToken = null;
        if (refreshTokenBackupRepository.existsByAuthAccount(guestAccount)) {
            RefreshTokenBackup backup = refreshTokenBackupRepository.findByAuthAccount(guestAccount)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
            oldHashedRefreshToken = backup.getHashedTokenValue();

            // 4-1. DB Backup 삭제
            refreshTokenBackupRepository.delete(backup);
        }

        // 4-2. Redis 삭제 (기존 토큰이 있었다면)
        if (oldHashedRefreshToken != null) {
            redisUtil.deleteHashedRefreshToken(oldHashedRefreshToken); // RedisUtil에 delete 메서드 필요 (없으면 setTTL(0))
        }

        /*
         * 4-3. Guest AuthAccount 삭제 (이제 Guest로는 로그인 불가)
         * - Cascade 설정에 따라 다르지만, 명시적으로 지워주는 것이 안전하다
         * - flush를 통해 삭제 쿼리를 즉시 실행하여 Unique 제약 조건 충돌 등을 방지한다.
         */
        authAccountRepository.delete(guestAccount);
        authAccountRepository.flush();

        // 5. User 정보 업데이트
        // Guest 시절엔 이메일이 없었으므로, Google 이메일로 채워줌 (User 엔티티에 setter 혹은 update 메서드 필요)
        user.updateEmail(email);

        // 6. 새로운 Google AuthAccount 생성 및 연결
        AuthAccount googleAuthAccount = createAuthAccount(user, Provider.GOOGLE, hashedGoogleSub);

        // 7. 새로운 토큰 발급 (Google Context)
        CustomUserDetails userDetails = buildCustomUserDetails(user, googleAuthAccount);
        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);

        // 8. 새 토큰 저장 (DB & Redis)
        processRefreshTokenSaving(user, googleAuthAccount, refreshTokenInfo);

        // 9. 반환 (Google이므로 Recover Token은 비워놓고 반환한다)
        return buildJwtTokenResDto(accessTokenInfo.token(), refreshTokenInfo.token(), null, user.getEmail());
    }


    // ==================================================================
    // [Private] 내부 메서드
    // ==================================================================

    /**
     * registerGuest, registerGoogle에서 사용하는 User Entity 생성 공통 로직
     * - AccountCode, Nickname을 자동으로 생성해준다.
     * - email은 선택적으로 넣을 수 있다.
     * - User 생성 후, userRepository에 save()까지 수행한 결과물을 반환한다.
     * @param email : Guest-null, Google-Token에 있는 Google Email 설정
     */
    private User createUser(String email){
        /*
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
        if(email != null){ user.updateEmail(email); }
        return userRepository.save(user); // AuthAccount -> User Entity의 연관관계 설정을 위해, UserRepository로 먼저 save한다.
    }

    /**
     * registerGuest, register Google에서 사용하는 AuthAccount Entity 생성 공통 로직
     * @param user 연동할 user
     * @param provider 해당 회원가입 주체
     * @param hashedDeviceUid Guest->hashedDeviceUid, Google->hashedGoogleId
     */
    private AuthAccount createAuthAccount(User user, Provider provider, String hashedDeviceUid){
        AuthAccount authAccount = AuthAccount.builder()
                .provider(provider)
                .hashedDeviceUid(hashedDeviceUid)
                .user(user)
                .build();
        return authAccountRepository.save(authAccount); // AuthAccount -> User Entity의 연관관계 설정을 위해, authRepository로 먼저 save한다.
    }

    /**
     * registerGuest - 6. Refresh Token을 RefreshTokenBackup Table에 추가(Update)
     * 새로 생성한 RefreshToken을 RefreshTokenBackup 테이블에 업데이트 하는 함수이다.
     * - 기존에 해당 authAccount PK 행이 존재하면 업데이트, 없다면 새로 추가한다.
     * [ 업데이트 로직 ]
     *  (1) authAccount.deviceUid -> refreshTokenBackup.deviceUid (authAccount 테이블이 deviceUid를 관리하는 책임자이다.)
     *  (2) TokenInfo.token -> refreshTokenBackup.tokenValue
     *  (3) TokenInfo.expiresAt -> refreshTokenBackup.expiresAt
     * [ 신규 추가 로직 ]
     *  (1) RefreshTokenBackup 새로 생성하여 authAccount.updateRefreshTokenBackup(...)으로 연결
     *  -> 영속성 컨텍스트의 Dirty Checking으로 트랜잭션 종료 시 Update됨
     * @param authAccount : 새로 생성한 AuthAccount 객체
     * @param hashedRefreshToken : 새로 발급한 refreshToken을 해싱한 결과값
     * @param expiresAt : refreshToken의 만료 시간
     */
    private void updateRefreshTokenBackupTable(AuthAccount authAccount, String hashedRefreshToken, LocalDateTime expiresAt){
        RefreshTokenBackup backup = null;
        /*
         * 1. RefreshTokenBackup Table에 해당 authAccount PK가 존재하는 경우
         * 기존에 해당 테이블에 authAccount Pk가 존재한다면, 기존 Row를 Update한다.
         */
        if(refreshTokenBackupRepository.existsByAuthAccount(authAccount)){
            backup = refreshTokenBackupRepository.findByAuthAccount(authAccount).orElseThrow(() -> new CustomException(AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND_IN_REFRESH_TOKEN_BACKUP));
            backup.updateBackupInfo(authAccount.getHashedDeviceUid(), hashedRefreshToken, expiresAt); // RefreshTokenBackup 테이블의 내용을 업데이트한다.
        }
        /*
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

    /**
     * key, value, expiresAt(절대시간)을 입력받아 redis에 등록해주는 함수
     */
    private void registerToRedis(String key, RedisValue value, LocalDateTime expiresAt){
        long ttlInMillis = getDurationInMillis(expiresAt); // 현재 ~ refreshToken의 만료시간까지 남은 밀리초 계산
        if (ttlInMillis < 0) { // 만료시간이 이미 지난 경우 방어 (음수를 Redis에 저장하면 에러날 수 있으므로)
            ttlInMillis = 0;
        }
        if(ttlInMillis > 0){
            redisUtil.setHashedRefreshTokenValue(key, value, ttlInMillis);
        }
    }

    /**
     * LocalDateTime (절대시간)까지 남은 시간을 millis로 반환해주는 함수
     * @param at : 목표 절대 시간
     */
    private long getDurationInMillis(LocalDateTime at){
        return Duration.between(LocalDateTime.now(), at).toMillis(); // 밀리초로 변환
    }

    /**
     * JwtTokenResDto를 빌드하는 함수
     */
    private JwtTokenResDto buildJwtTokenResDto(String accessToken, String refreshToken, String recoverToken, String email){
        return JwtTokenResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .recoverToken(recoverToken)
                .email(email)
                .build();
    }

    /**
     * RedisValue를 빌드하는 함수
     * @param subject : Access Token, Refresh Token, Recover Token을 생성하는데에 사용되는 subject (provider:hashedDeviceUid)
     * @param role : User의 Role
     * @param expiresAt : Refresh Token의 만료 절대 시간
     * @return RedisValue
     */
    private RedisValue buildRedisValue(Long userId, Long authAccountId, String subject, RoleType role, LocalDateTime expiresAt){
        return RedisValue.builder()
                .userId(userId)
                .authAccountId(authAccountId)
                .subject(subject) // provider:deviceUid
                .role(role) // RoleType Enum
                .expiresAt(expiresAt) // Refresh Token 만료 절대 시간
                .build();
    }

    /**
     * HashedDeviceUid & Provider로 AuthAccount 객체를 찾는 함수
     * @param hashedDeviceUid String
     * @param provider Provider
     * @return AuthAccount
     */
    private AuthAccount findAuthAccountByHashedDeviceUidAndProvider(String hashedDeviceUid, Provider provider){
        return authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, provider)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND));
    }

    /**
     * CustomUserDetails 생성 함수
     * @param user : User
     * @param authAccount : AuthAccount
     * @return CustomUserDetails
     */
    private CustomUserDetails buildCustomUserDetails(User user, AuthAccount authAccount){
        UserAuthDto userAuthDto = UserAuthDto.builder()
                .userId(user.getId())
                .authAccountId(authAccount.getId())
                .provider(authAccount.getProvider())
                .hashedDeviceUid(authAccount.getHashedDeviceUid()) // authAccount에 들어있는 deviceUid는 해싱된 상태이다.
                .role(user.getRole())
                .build();
        return new CustomUserDetails(userAuthDto);
    }

    /**
     * 모든 토큰들을 발급한 뒤, Refresh Token을 Table, Redis에 저장하는 Process를 수행해주는 함수
     */
    private void processRefreshTokenSaving(User user, AuthAccount authAccount, TokenInfo refreshTokenInfo){
        // 1. Refresh Token을 RefreshTokenBackup Table에 추가(Update)
        String hashedRefreshToken = hashingUtil.hashingValue(refreshTokenInfo.token());
        updateRefreshTokenBackupTable(authAccount, hashedRefreshToken, refreshTokenInfo.expiresAt());

        /*
         * 2. Redis에 해당 RefreshToken 저장
         * - Key: hashedRefreshToken
         * - Value: RedisValue Record Dto
         */
        RedisValue redisValue = buildRedisValue(
                user.getId(),
                authAccount.getId(),
                jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid()),
                user.getRole(),
                refreshTokenInfo.expiresAt()
        );
        registerToRedis(hashedRefreshToken, redisValue, refreshTokenInfo.expiresAt());
    }

    /**
     * 사용자가 회원가입을 한 뒤, 기본적으로 사용자에게 할당해줘야할 것들을 할당해주는 함수.
     *  (1) 기본 UserItem 인스턴스 생성
     *  (2) 기본 Ability 인스턴스 생성
     *  (3) UserTerm 인스턴스 생성
     */
    private void allocateUser(User user){
        // 1. 사용자 UserItems 생성
        createDefaultUserItems(user);

        // 2. 사용자 Ability 생성
        createDefaultAbility(user);

        // 3. 사용자 약관 동의
        createDefaultUserTerms(user);

        // 4. Mission 생성
        createDefaultMission(user);
    }


    private List<UserItem> createDefaultUserItems(User user){
        /*
         * 1. Item들 중 name이 "Default.png"인 데이터들을 조회한다.
         * - 기본 아이템들을 못찾으면 예외처리
         */
        List<Item> defaultItems = itemRepository.findAllByName("Default.png");
        if(defaultItems.isEmpty()){throw new CustomException(ShopErrorCode.DEFAULT_ITEMS_NOT_FOUND);}

        defaultItems.forEach(defaultItem -> {log.info("{}", defaultItem.getFullUrl("example"));});


        // 2. 찾은 아이템들로 UserItem들을 생성해서 테이블에 저장
        List<UserItem> userItems = defaultItems.stream().map(item ->
                UserItem.builder()
                    .user(user)
                    .item(item)
                    .isEquipped(true)
                    .build()
        ).toList();
        return userItemRepository.saveAll(userItems);
    }

    /**
     * user 생성 시 ability 생성 로직 (능력치)
     * @param user 연동할 user
     */
    private Ability createDefaultAbility(User user) {
        Ability ability = Ability.builder()
                .user(user)
                .build();
        return abilityRepository.save(ability);
    }

    /**
     * user 생성 시 할당해줄
     */
    private List<UserTerm> createDefaultUserTerms(User user){
        // 1. 모든 약관들을 가져온다.
        List<Term> terms = termRepository.findAll();
        if(terms.isEmpty()){throw new CustomException(TermErrorCode.TERMS_NOT_FOUND);}

        // 2. 새로 만든 User와 가져온 Term들을 연결한다.
        List<UserTerm> userTerms = new ArrayList<>();
        for(Term term : terms){
            userTerms.add(
                    UserTerm.builder()
                            .term(term)
                            .user(user)
                            .build()
            );
        }
        return userTermRepository.saveAll(userTerms);
    }

    private Mission createDefaultMission(User user){
        Mission mission = Mission.builder()
                .user(user)
                .build();
        return missionRepository.save(mission);
    }
}
