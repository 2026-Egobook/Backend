package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.admin.entity.Admin;
import com.example.egobook_be.domain.admin.enums.AdminApprovalStatus;
import com.example.egobook_be.domain.admin.exception.AdminAuthErrorCode;
import com.example.egobook_be.domain.admin.repository.AdminRepository;
import com.example.egobook_be.domain.auth.dto.req.AdminAuthReqDto;
import com.example.egobook_be.domain.auth.dto.req.RefreshReqDto;
import com.example.egobook_be.domain.auth.dto.res.AdminLoginResDto;
import com.example.egobook_be.domain.auth.dto.res.JwtTokenResDto;
import com.example.egobook_be.domain.user.enums.RoleType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final TokenManagementService tokenManagementService;
    private final JwtUtil jwtUtil;
    private final HashingUtil hashingUtil;
    private final RedisUtil redisUtil;

    /**
     * 관리자가 회원가입을 신청하는 함수
     * @param reqDto 회원가입 요청 DTO
     */
    @Transactional
    public void registerAdmin(AdminAuthReqDto reqDto) {
        if (adminRepository.existsByAdminId(reqDto.adminId())) {
            throw new CustomException(AdminAuthErrorCode.ADMIN_ID_ALREADY_EXISTS);
        }

        String hashedPassword = hashingUtil.hashingValue(reqDto.password());
        Admin admin = Admin.builder()
                .adminId(reqDto.adminId())
                .password(hashedPassword)
                .build();
        adminRepository.save(admin);
    }

    /**
     * 관리자 로그인 함수
     * @param reqDto 로그인 요청 DTO
     * @return 관리자 로그인 응답 DTO
     */
    @Transactional(readOnly = true)
    public AdminLoginResDto loginAdmin(AdminAuthReqDto reqDto) {
        Admin admin = adminRepository.findByAdminId(reqDto.adminId())
                .orElseThrow(() -> new CustomException(AdminAuthErrorCode.ADMIN_LOGIN_FAILED));

        String hashedPassword = hashingUtil.hashingValue(reqDto.password());
        if (!hashedPassword.equals(admin.getPassword())) {
            throw new CustomException(AdminAuthErrorCode.ADMIN_LOGIN_FAILED);
        }

        validateAdminApprovalStatus(admin.getApprovalStatus());

        UserAuthDto userAuthDto = UserAuthDto.ofAdmin(admin.getId(), admin.getAdminId(), admin.getRole());
        CustomUserDetails customUserDetails = new CustomUserDetails(userAuthDto);

        TokenInfo accessTokenInfo = jwtUtil.createAccessToken(customUserDetails);
        TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(customUserDetails);

        String hashedRefreshToken = hashingUtil.hashingValue(refreshTokenInfo.token());
        RedisValue redisValue = RedisValue.builder()
                .userId(null)
                .authAccountId(null)
                .subject(customUserDetails.getUsername())
                .role(admin.getRole())
                .expiresAt(refreshTokenInfo.expiresAt())
                .build();
        long ttlInMillis = jwtUtil.getExpirationInMs(refreshTokenInfo.token()) - System.currentTimeMillis();
        if (ttlInMillis > 0) {
            redisUtil.setHashedRefreshTokenValue(hashedRefreshToken, redisValue, ttlInMillis);
        }

        return AdminLoginResDto.builder()
                .accessToken(accessTokenInfo.token())
                .refreshToken(refreshTokenInfo.token())
                .build();
    }

    /**
     * 관리자용 Access Token 재발급
     * Redis에 저장된 Refresh Token 정보만 사용한다.
     * @param reqDto RefreshReqDto (accessToken, refreshToken)
     * @return JwtTokenResDto (새 accessToken, 기존 refreshToken)
     */
    @Transactional
    public JwtTokenResDto refreshAdminToken(RefreshReqDto reqDto) {
        String hashedRefreshToken = hashingUtil.hashingValue(reqDto.refreshToken());
        RedisValue redisValue = redisUtil.getHashedRefreshTokenValue(hashedRefreshToken);

        if (redisValue == null) {
            throw new CustomException(AdminAuthErrorCode.ADMIN_REFRESH_TOKEN_NOT_FOUND);
        }

        validateAdminRole(redisValue.role());
        validateAdminSubject(redisValue.subject());

        tokenManagementService.addAccessTokenInRedisBlackList(reqDto.accessToken());

        TokenInfo newAccessTokenInfo = jwtUtil.createAccessToken(
                redisValue.userId(),
                redisValue.authAccountId(),
                redisValue.subject(),
                redisValue.role()
        );

        return JwtTokenResDto.builder()
                .accessToken(newAccessTokenInfo.token())
                .refreshToken(reqDto.refreshToken())
                .recoverToken(null)
                .email(null)
                .build();
    }

    private void validateAdminApprovalStatus(AdminApprovalStatus approvalStatus) {
        if (approvalStatus == AdminApprovalStatus.PENDING) {
            throw new CustomException(AdminAuthErrorCode.ADMIN_NOT_APPROVED);
        }
        if (approvalStatus == AdminApprovalStatus.REJECTED) {
            throw new CustomException(AdminAuthErrorCode.ADMIN_SIGNUP_REJECTED);
        }
    }

    private void validateAdminRole(RoleType role) {
        if (role != RoleType.ROLE_ADMIN) {
            throw new CustomException(AdminAuthErrorCode.INVALID_ADMIN_REFRESH_TOKEN);
        }
    }

    private void validateAdminSubject(String subject) {
        if (subject == null || !subject.startsWith("ADMIN:")) {
            throw new CustomException(AdminAuthErrorCode.INVALID_ADMIN_REFRESH_TOKEN);
        }
    }
}
