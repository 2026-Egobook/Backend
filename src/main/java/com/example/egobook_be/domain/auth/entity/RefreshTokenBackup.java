package com.example.egobook_be.domain.auth.entity;

import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Table(name = "refresh_token_backup") // DB 테이블명 명시
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 보호 (JPA 필수)
public class RefreshTokenBackup extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기기 고유 ID (어떤 기기의 토큰인지 식별)
     */
    @Column(name = "hashed_device_uid", nullable = false)
    private String hashedDeviceUid; // HmacSHA256 방식으로 해싱된 recoverToken

    /**
     * 리프레시 토큰 값
     * JWT는 생각보다 길기에, ERD에는 VARCHAR(255)로 되어있으나, 서명 알고리즘과 페이로드 크기에 따라 255자를 넘을 수 있다.
     * 안전하게 길이를 1000자 이상으로 설정하였다.
     */
    @Column(name = "hashed_token_value", nullable = false, length = 1000)
    private String hashedTokenValue; // HmacSHA256 방식으로 해싱된 recoverToken

    /**
     * 만료 시간 (토큰 자체의 exp 클레임과 일치시켜야 함)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * [RefreshTokenBackup -> AuthAccount]
     * Cascade는 부모 쪽에서 관리함
     * 하나의 로그인 계정(AuthAccount)은 하나의 RefreshToken을 가질 수 있도록 하기 위해, unique 설정을 해주었음
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_account_id", nullable = false, unique = true)
    private AuthAccount authAccount;

    /**
     * Refresh Token Rotation(RTR) 시, 엔티티를 새로 만들지 않고 기존 백업 데이터를 업데이트(Dirty Checking)하기 위한 메서드
     * deviceUid, tokenValue, expiresAt을 최신화한다.
     */
    public void updateBackupInfo(String hashedDeviceUid, String hashedTokenValue, LocalDateTime newExpiry) {
        this.hashedDeviceUid = hashedDeviceUid;
        this.hashedTokenValue = hashedTokenValue;
        this.expiresAt = newExpiry;
    }
}
