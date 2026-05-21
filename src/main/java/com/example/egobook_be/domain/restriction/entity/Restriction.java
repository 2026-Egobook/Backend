package com.example.egobook_be.domain.restriction.entity;

import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "Restriction")
public class Restriction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restrictionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RestrictionDomainType domainType;

    // [AI-MOD] 제재 사유를 문자열로 저장
    @Column(nullable = false, length = 50)
    private String reason;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RestrictionStatus status = RestrictionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime restrictionUntil;

    // [AI-MOD] 제재 생성 reason 타입 문자열로 변경
    public static Restriction create(Long adminId, Long userId,
            RestrictionDomainType domainType, String reason,
            String description) {
        return Restriction.builder()
                .adminId(adminId)
                .userId(userId)
                .domainType(domainType)
                .reason(reason)
                .description(description)
                .status(RestrictionStatus.ACTIVE)
                .restrictionUntil(LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(7))
                .build();
    }

    // ACTIVE 상태인 제재 해제 처리
    public void cancel() {
        this.status = RestrictionStatus.CANCELED;
    }
}
