package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.user.dto.FcmTokenReqDto;
import com.example.egobook_be.domain.user.dto.UserNicknameResDto;
import com.example.egobook_be.domain.user.dto.UserNicknameUpdateReqDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final RedisUtil redisUtil;

    @Value("${app.data.purge-duration-in-ms}")
    private Long purgeDurationInMs;

    @Transactional
    public UserNicknameResDto updateNickname(Long userId, UserNicknameUpdateReqDto reqDto) {
        // 1. User 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 닉네임 업데이트 (해당 닉네임의 스타일은 이미 Dto 레벨에서 검증되었음)
        user.updateNickname(reqDto.nickname());

        return UserNicknameResDto.builder().newNickname(user.getNickname()).build();
    }

    /**
     * 회원 탈퇴 로직을 수행하는 함수 (Soft Delete + 사용자 인증 데이터 삭제)
     * @param userId 탈퇴한 사용자 ID
     * @param accessToken 현재 요청에 사용된 Access Token
     */
    @Transactional
    public void withDrawAccount(Long userId, String accessToken){
        // 1. 사용자 인스턴스 가져오기 (비관적 락), 해당 사용자의 인증 정보 가져오기
        User user = userRepository.findByIdWithLock(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        AuthAccount userAuthAccount = authAccountRepository.findByUser(user).orElseThrow(() -> new CustomException(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND));

        // 2. 이미 탈퇴 대기 중인 상태인지 확인(멱등성 보장)
        if(user.getStatus().equals(UserStatus.WITHDRAW_PENDING)){
            throw new CustomException(UserErrorCode.ALREADY_WITHDRAW_PENDING);
        }

        /*
         * 3. 해당 사용자 상태 최신화(비식별화)
         * (1) status -> WITHDRAW_PENDING
         * (2) deletedAt(삭제 요청 시각) 최신화
         * (3) purgeAt(완전 삭제 예정 시각) 최신화
         * (4) dailPraise (AI 칭찬서 수신 여부) false
         * (5) notificationEnabled (알림 설정) false
         */
        user.withdrawUser(purgeDurationInMs);

        // 4. 발급 받은 Access Token JTI Redis BlackList에 등록

        redisUtil.setTokenInBlacklist(resolveToken(accessToken));

        /*
         * 5. Redis에 저장된 Refresh Token 즉시 삭제 & 사용자와 연관된 RefreshTokenBackup 테이블 데이터 삭제
         * - 만약 refreshTokenBackup 테이블에 해당 내용이 없더라도, 회원탈퇴 로직을 계속해서 수행해야함
         * - 해당 사용자와 연관된 AuthAccount는 아직 삭제하지 않는다.(7일 뒤 스케줄러로 삭제한다)
         */
        List<RefreshTokenBackup> backups = refreshTokenBackupRepository.findAllByAuthAccount(userAuthAccount);

        // 5-1. Redis에서 각 토큰 삭제 (반복문)
        for (RefreshTokenBackup backup : backups) {
            redisUtil.deleteHashedRefreshToken(backup.getHashedTokenValue());
        }

        // 5-2. orphanRemoval 설정 활용하여 refreshTokenBackup 객체 삭제
        userAuthAccount.updateRefreshTokenBackup(null);

    }

    private String resolveToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    /** FCM 토큰 업데이트 */
    @Transactional
    public void updateFcmToken(Long userId, FcmTokenReqDto dto) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(UserErrorCode.USER_NOT_FOUND)
        );

        user.updateFcmToken(dto.fcmToken());
    }
}
