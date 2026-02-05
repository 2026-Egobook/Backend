package com.example.egobook_be.domain.ads.entity;

import com.example.egobook_be.domain.ads.enums.AdStatus;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ads")
public class Ads extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================
    // [ 1. 미디어 & 식별 컬럼 ]
    // ========================================================
    @Column(nullable = false, length = 100)
    private String advertiserName; // 광고주 이름

    @Column(nullable = false, length = 100)
    private String advertiserEmail; // 광고주 이메일

    @Column(nullable = false)
    private String cloudinaryPublicId; // 해당 광고의 Cloudinary의 public id

    @Column(nullable = false, length = 100)
    private String title; // 광고명

    @Column
    private String description; // 광고 설명

    @Column(length = 50)
    private String ctaText; // 해당 광고의 버튼 유도 문구

    @Column(nullable = false, length = 2048)
    private String landingUrl; // 광고의 버튼 클릭 시 이동할 URL

    // ========================================================
    // [ 2. 재생 규칙 컬럼 ]
    // ========================================================
    @Column(nullable = false)
    private Double videoDurationSec; // 광고 영상 길이

    @Column(nullable = false)
    @Builder.Default
    private Double rewardGrantSec = 15.0; // 광고 시청 보상 기준 시간(해당 시간 이상으로 봐야 보상 수령 가능)

    // ========================================================
    // [ 3. 돈 & 예산 컬럼 ]
    // ========================================================
    @Column(nullable = false)
    private Integer totalBudget; // 총 예산 (처음 계약 시, 해당 광고가 얼마짜리 광고였는지 기록되는 컬럼)

    @Column(nullable = false)
    private Integer remainingBudget; // 잔여 예산 (광고가 송출될 때마다 실시간으로 깎인다.)

    @Column(nullable = false)
    private Integer costPerView; // 시청 단가 (광고 1회 시청 시 광고주가 내야하는 돈)

    @Column(nullable = false)
    private Integer rewardInk; // 사용자가 해당 광고 시청 시 얻을 수 있는 잉크 보상 양

    // ========================================================
    // [ 4. 상태 & 스케줄링 컬럼 ]
    // ========================================================
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private AdStatus status = AdStatus.PENDING; // 광고 상태 (기본값은 대기 상태)

    @Column
    private LocalDateTime startAt; // 해당 광고 적용이 시작되는 시간

    @Column
    private LocalDateTime endAt; // 해당 광고 적용이 종료되는 시간 (계약 종료 날짜)

    // ========================================================
    // [ 5. 동시성 제어용 컬럼 ] - 낙관적 락(Optimistic Locking)
    // ========================================================
    @Version
    private Long version;

    // ========================================================
    // [ 비즈니스 로직 ]
    // ========================================================

}
