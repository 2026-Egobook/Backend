package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.HashingUtil;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.module.RedisValue;
import com.example.egobook_be.global.util.module.TokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

// Token을 관리하는 Sevice
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenManagementService {
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final JwtUtil jwtUtil;
    private final HashingUtil hashingUtil;
    private final RedisUtil redisUtil;

    /**
     * 재인증 및 계정 연동 시, 기존에 사용하던 토큰들을 모두 무효화하는 함수
     * @param accessToken : 무효화할 기존 Access Token
     * @param authAccount : 조회할 기존 AuthAccount
     * @param userId : 로깅용 User ID
     */
    @Transactional
    public void invalidatePreviousTokens(String accessToken, AuthAccount authAccount, Long userId) {
        // 1. Access Token 블랙리스트 등록
        if (accessToken != null && !accessToken.isBlank()) {
            addAccessTokenInRedisBlackList(accessToken);
        }

        // 2. 기존 Refresh Token 백업 조회 후 Redis 및 DB에서 삭제
        refreshTokenBackupRepository.findByAuthAccount(authAccount).ifPresent(refreshTokenBackup -> {
            // Redis에서 삭제
            deleteOldRefreshTokenFromRedis(refreshTokenBackup, userId);

            // DB에서 삭제
            refreshTokenBackupRepository.delete(refreshTokenBackup);
            log.info("🗑️ [TokenManagement] 기존 Refresh Token Backup을 DB에서 삭제했습니다. User: {}", userId);
        });
    }

    /**
     * 이전 버전의 RefreshToken(Refresh Token Backup Table에 있는 Refresh Token)이 Redis에 남아있다면 Redis에서 지우는 함수
     * @param refreshTokenBackup : 확인할 Refresh Token Backup Table
     * @param userId : 로깅할 userId
     */
    public void deleteOldRefreshTokenFromRedis(RefreshTokenBackup refreshTokenBackup, Long userId) {
        String oldHashedToken = refreshTokenBackup.getHashedTokenValue();
        // RedisUtil을 통해 삭제 (존재하면 삭제, 없으면 무시됨)
        if (oldHashedToken != null) {
            redisUtil.deleteHashedRefreshToken(oldHashedToken);
            log.info("🔄 [Rotation] 기존 Refresh Token을 Redis에서 삭제했습니다. User: {}", userId);
        }
    }

    /**
     *  HttpServletRequest에 들어있는 AccessToken를 추출하고 블랙리스트에 등록하는 함수
     * @param accessToken : 블랙리스트에 등록할 엑세스 토큰
     */
    public void addAccessTokenInRedisBlackList(String accessToken){
        // 만료되지 않은 토큰이라도 강제로 블랙리스트 처리
        redisUtil.setTokenInBlacklist(accessToken);
        log.info("🪪 재발급 요청에 사용된 기존 Access Token을 블랙리스트에 등록했습니다.");
    }

    /**
     * RefreshTokenBackup 테이블에 있던 Hashed Refresh Token을 Redis에 복구시키는 함수
     * @param hashedRefreshToken : 해싱된 RefreshToken
     * @param user : 해싱된 RefreshToken Key의 Value에 넣을 데이터를 위한 사용자
     * @param authAccount : DB에 저장되어있던 해싱된 RefreshToken Key의 Value에 넣을 데이터를 위한 AuthAccount
     * @param expiresAt : DB에 저장되어있던 해싱된 Refresh Token 만료 시간
     */
    @Transactional
    public void restoreHashedRefreshTokenRedisValue(String hashedRefreshToken, User user, AuthAccount authAccount, LocalDateTime expiresAt) {
        String subject = jwtUtil.createSubject(authAccount.getProvider(), authAccount.getHashedDeviceUid());
        RedisValue restoreRedisValue = buildRedisValue(user.getId(), authAccount.getId(), subject, user.getRole(), expiresAt); // RedisValue 생성
        registerToRedis(hashedRefreshToken, restoreRedisValue, expiresAt); // Redis에 해당 데이터들 복구
        log.info("🔄 [TokenManagement] Redis 복원 성공 - userId: {}, authAccountId: {}", user.getId(), authAccount.getId());
    }

    /**
     * Refresh Token을 RefreshTokenBackup Table과 Redis에 저장하는 함수
     * @param refreshTokenInfo : 저장할 Refresh Token의 정보를 담고 있는 Dto
     * @param user : Redis에 Hashed RefreshToken Key와 함께 저장될 Value를 채울 User 정보
     * @param authAccount :  Redis에 Hashed RefreshToken Key와 함께 저장될 Value를 채울 AuthAccount 정보
     */
    @Transactional
    public void saveRefreshTokenToTableAndRedis(TokenInfo refreshTokenInfo, User user, AuthAccount authAccount){
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
        log.info("💾 [TokenManagement] DB/Redis 토큰 저장 완료 - userId: {}, authAccountId: {}", user.getId(), authAccount.getId());
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
        refreshTokenBackupRepository.findByAuthAccount(authAccount)
                .ifPresentOrElse(
                        /*
                         * 1. RefreshTokenBackup Table에 해당 authAccount PK가 존재하는 경우
                         * 기존에 해당 테이블에 authAccount Pk가 존재한다면, 기존 Row를 Update한다.
                         * => findBy 쿼리 한 번으로 조회 후, 분기 처리한다.
                         */
                        existingBackup -> {
                            existingBackup.updateBackupInfo(authAccount.getHashedDeviceUid(), hashedRefreshToken, expiresAt); // RefreshTokenBackup 테이블의 내용을 업데이트한다.
                        },
                        /*
                         * 2. RefreshTokenBackup Table에 해당 authAccount PK가 존재하지 않는 경우
                         * 새로운 RefreshTokenBackup 객체를 생성하여 authAccount에 연관관계를 추가한다.
                         */
                        () -> {
                            RefreshTokenBackup newBackup = RefreshTokenBackup.builder()
                                    .authAccount(authAccount)
                                    .hashedDeviceUid(authAccount.getHashedDeviceUid()) // 이미 암호화된 deviceUid이므로, 한번 더 해싱하면 안된다.
                                    .hashedTokenValue(hashedRefreshToken) // 암호화된 refreshToken을 저장한다.
                                    .expiresAt(expiresAt)
                                    .build();
                            authAccount.updateRefreshTokenBackup(newBackup);
                        }
                );
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

}
