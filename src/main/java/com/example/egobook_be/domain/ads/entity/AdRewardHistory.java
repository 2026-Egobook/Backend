package com.example.egobook_be.domain.ads.entity;

import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import lombok.*;

import jakarta.persistence.*; // Spring Boot 3.x 기준 (2.x라면 javax.persistence)


@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_reward_history", indexes = {
        /*
         * [ Transaction 중복 검증을 위한 DB 인덱스 ]
         * - transaction_id는 절대 중복될 수 없음 (DB 레벨 방어)
         */
        @Index(name = "idx_transaction_id", columnList = "transactionId", unique = true),
        /*
         * [ 사용자의 오늘 남은 광고 호출 횟수를 계산하기 위한 DB 인덱스 ]
         * - 조회 성능을 위해 user_id와 날짜로 인덱스 생성
         */
        @Index(name = "idx_user_created", columnList = "user_id, createdAt")
})
public class AdRewardHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AdMob이 주는 고유 영수증 번호
    @Column(nullable = false, length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 받은 보상 양
    @Column(nullable = false)
    private int rewardAmount;

    // 보상 타입
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdRewardType rewardType;

    // 어떤 광고 단위인지의 ID (전면(Interstitial), 리워드(Reward)...)
    @Column(nullable = false, length = 100)
    private String adUnitId;

    // 주간 보고서와의 연관관계(Nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_counsel", nullable = true)
    private WeeklyCounsel weeklyCounsel;
}