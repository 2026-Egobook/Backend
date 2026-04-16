package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.dto.req.GuestRecertificationReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
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
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.security.CustomUserDetails;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.module.RedisValue;
import com.example.egobook_be.global.util.module.TokenInfo;
import com.example.egobook_be.global.util.module.UserAuthDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final TokenManagementService tokenManagementService;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final JwtUtil jwtUtil;
    private final HashingUtil hashingUtil;
    private final RedisUtil redisUtil;

    @Value("${app.data.purge-duration-in-ms}")
    private Long purgeDurationInMs;

    /**
     * 관리자용 Access Token 재발급 (FallBack 전략 적용)
     *
     * 1. Hashed Refresh Token으로 Redis 조회 시도
     *    → 성공 시: 기존 Access Token 블랙리스트 처리 후 새 Access Token 즉시 발급
     * 2. Redis 조회 실패 시 → RefreshTokenBackup 테이블 조회 (Fallback)
     * 3. DB 조회 성공 시 → 만료 여부 검증 후 Redis 복구 및 새 Access Token 발급
     * 4. DB에서도 찾지 못하거나 만료된 경우 → 재로그인 안내 에러 반환
     *
     * @param reqDto RefreshReqDto (accessToken, refreshToken)
     * @return JwtTokenResDto (새 accessToken, 기존 refreshToken)
     */
    @Transactional
    public JwtTokenResDto refreshAdminToken(RefreshReqDto reqDto) {
        // 1. 전달받은 Refresh Token 해싱
        String hashedRefreshToken = hashingUtil.hashingValue(reqDto.refreshToken());

        /*
         * 2. Redis에서 조회 시도
         * - 조회 성공 시: 기존 Access Token을 블랙리스트에 등록하고 새 Access Token 발급
         */
        RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken);
        if (redisValue != null) {
            validateAdminRole(redisValue.role());
            tokenManagementService.addAccessTokenInRedisBlackList(reqDto.accessToken());
            TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(
                    redisValue.userId(), redisValue.authAccountId(), redisValue.subject(), redisValue.role()
            );
            return buildJwtTokenResDto(newAccessTokenInfo.token(), reqDto.refreshToken());
        }

        /*
         * 3. Redis 조회 실패 시 RefreshTokenBackup 테이블에서 조회 (Fallback)
         */
        log.warn("[AdminAuthService] Redis에서 HashedRefreshToken을 찾을 수 없습니다. RefreshTokenBackup Table을 조회합니다.");
        RefreshTokenBackup backup = refreshTokenBackupRepository.findByHashedTokenValue(hashedRefreshToken)
                .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        /*
         * 4. DB에서 가져온 Refresh Token의 만료 여부 검증
         * - 만료된 경우: 관리자 계정으로 재로그인 안내
         */
        if (backup.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED_ADMIN);
        }

        /*
         * 5. 모든 검증 통과 시 Redis 복구 후 새 Access Token 발급
         * - DB에 저장된 Refresh Token 정보를 Redis에 복구한다.
         */
        AuthAccount authAccount = backup.getAuthAccount();
        User user = authAccount.getUser();

        validateAdminRole(user.getRole());

        tokenManagementService.restoreHashedRefreshTokenRedisValue(hashedRefreshToken, user, authAccount, backup.getExpiresAt());

        // 6. 기존 Access Token Redis 블랙리스트에 등록
        tokenManagementService.addAccessTokenInRedisBlackList(reqDto.accessToken());

        // 7. 새 Access Token 발급 후 반환
        String subject = jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid());
        TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(user.getId(), authAccount.getId(), subject, user.getRole());
        return buildJwtTokenResDto(newAccessTokenInfo.token(), reqDto.refreshToken());
    }

    /**
     * 관리자용 Recover Token으로 Refresh Token 재발급
     *
     * Refresh Token이 만료된 관리자가 기기에 영구 저장된 Recover Token으로 세션을 복구하는 함수.
     * AuthService.recertificationGuestToken()과 동일한 흐름이며, ROLE_ADMIN 검증이 추가된다.
     *
     * 1. deviceUid, recoverToken 해싱
     * 2. AuthAccount 조회 (Provider.GUEST)
     * 3. ROLE_ADMIN 검증
     * 4. Recover Token 일치 여부 검증 (불일치 시 계정 삭제 대기 처리)
     * 5. 계정 탈퇴 대기 상태 여부 확인
     * 6. 기존 토큰 무효화 (Access 블랙리스트 + Refresh Redis/DB 삭제)
     * 7. 새 Access, Refresh, Recover Token 발급 및 저장
     *
     * @param reqDto GuestRecertificationReqDto (deviceUid, accessToken, recoverToken)
     * @return JwtTokenResDto (새 accessToken, refreshToken, recoverToken)
     */
    @Transactional
    public JwtTokenResDto recertificationAdminToken(GuestRecertificationReqDto reqDto) {
        // 1. deviceUid, recoverToken 해싱
        String hashedDeviceUid = hashingUtil.hashingValue(reqDto.deviceUid());
        String hashedRecoverToken = hashingUtil.hashingValue(reqDto.recoverToken());

        // 2. AuthAccount 조회 (관리자 계정은 GUEST Provider 기반으로 Recover Token을 발급받는다)
        AuthAccount authAccount = authAccountRepository.findByHashedDeviceUidAndProvider(hashedDeviceUid, Provider.GUEST)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND));

        User user = authAccount.getUser();

        // 3. ROLE_ADMIN 검증
        validateAdminRole(user.getRole());

        /*
         * 4. Recover Token 일치 여부 검증
         * - 불일치 시: 비정상 접근으로 간주하여 계정을 삭제 대기 상태로 전환
         */
        if (!authAccount.getHashedRecoverToken().equals(hashedRecoverToken)) {
            log.warn("[AdminAuthService] HashedDeviceUid: {}, 유효하지 않은 Recover Token으로 복구 시도를 하였습니다.", hashedDeviceUid);
            user.withdrawUser(purgeDurationInMs);
            throw new CustomException(AuthErrorCode.INVALID_RECOVER_TOKEN);
        }

        // 5. 탈퇴 대기 상태 검증
        if (user.getStatus().equals(UserStatus.WITHDRAW_PENDING)) {
            throw new CustomException(AuthErrorCode.RECERTIFICATION_FAIL_USER_WITHDRAW_PENDING);
        }

        /*
         * 6. 기존 토큰 무효화
         * (1) 기존 Access Token 블랙리스트 등록
         * (2) 기존 Refresh Token Redis/DB에서 삭제
         */
        tokenManagementService.invalidatePreviousTokens(reqDto.accessToken(), authAccount, user.getId());

        /*
         * 7. 새 Access, Refresh, Recover Token 발급
         * - Recover Token 갱신 후 AuthAccount에 저장
         */
        CustomUserDetails userDetails = buildCustomUserDetails(user, authAccount);
        String subject = jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid());
        TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(user.getId(), authAccount.getId(), subject, user.getRole());
        TokenInfo newRefreshTokenInfo = jwtUtil.createRefreshToken(subject, user.getRole());
        TokenInfo newRecoverTokenInfo = jwtUtil.createRecoverToken(userDetails);
        authAccount.updateHashedRecoverToken(hashingUtil.hashingValue(newRecoverTokenInfo.token()));

        // 8. 새 Refresh Token을 DB & Redis에 저장
        tokenManagementService.saveRefreshTokenToTableAndRedis(newRefreshTokenInfo, user, authAccount);

        return buildJwtTokenResDto(newAccessTokenInfo.token(), newRefreshTokenInfo.token(), newRecoverTokenInfo.token());
    }

    /**
     * 요청한 토큰의 Role이 ROLE_ADMIN인지 검증하는 함수
     * Security 레이어에서 이미 차단되지만, 서비스 레이어에서도 이중으로 검증한다.
     */
    private void validateAdminRole(RoleType role) {
        if (role != RoleType.ROLE_ADMIN) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
    }

    private CustomUserDetails buildCustomUserDetails(User user, AuthAccount authAccount) {
        UserAuthDto userAuthDto = UserAuthDto.builder()
                .userId(user.getId())
                .authAccountId(authAccount.getId())
                .provider(authAccount.getProvider())
                .hashedDeviceUid(authAccount.getHashedDeviceUid())
                .role(user.getRole())
                .build();
        return new CustomUserDetails(userAuthDto);
    }

    private JwtTokenResDto buildJwtTokenResDto(String accessToken, String refreshToken) {
        return JwtTokenResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .recoverToken(null)
                .email(null)
                .build();
    }

    private JwtTokenResDto buildJwtTokenResDto(String accessToken, String refreshToken, String recoverToken) {
        return JwtTokenResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .recoverToken(recoverToken)
                .email(null)
                .build();
    }
}
