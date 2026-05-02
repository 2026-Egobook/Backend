package com.example.egobook_be.global.util.module;

import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.user.enums.RoleType;
import lombok.Builder;

/**
 * [JWT 토큰을 생성할 때 필요한 사용자 인증 정보들을 담고 있는 Dto]
 * Entity들을 직접 CustomUserDetails에 넣으면 트랜잭션 범위 밖에서의 Lazy Loading이 발생할 수 있다.
 * 따라서, 이 DTO를 사용하여 그런 문제를 방지한다.
 * @param userId : User Entity PK (Admin은 null)
 * @param authAccountId : AuthAccount Entity PK (Admin은 null)
 * @param adminId : Admin Entity PK (User는 null)
 * @param subject : JWT Subject ("ADMIN:adminId" 또는 "PROVIDER:hashedDeviceUid")
 * @param provider : Provider (User 전용, Admin은 null)
 * @param hashedDeviceUid : 해싱된 기기의 고유 UID (User 전용, Admin은 null)
 * @param role : 사용자 권한
 */
@Builder
public record UserAuthDto(
        Long userId,
        Long authAccountId,
        Long adminId,
        String subject,
        Provider provider,
        String hashedDeviceUid,
        RoleType role
) {
    public static UserAuthDto ofAdmin(Long adminId, String adminLoginId, RoleType role) {
        return UserAuthDto.builder()
                .adminId(adminId)
                .subject("ADMIN:" + adminLoginId)
                .role(role)
                .build();
    }
}
