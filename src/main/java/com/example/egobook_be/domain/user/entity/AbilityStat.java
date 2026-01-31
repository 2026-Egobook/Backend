package com.example.egobook_be.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AbilityStat {
    private static final int MAX_LEVEL = 300;
    private static final int SCORE_FOR_LEVEL_UP = 5;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1; // 기본 레벨 1

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0; // 기본 점수 0

    // =======================================================
    // 경험치 획득 및 레벨업 로직 캡슐화
    // =======================================================
    public int addScore(int amount) {
        // 만렙이면 성장 중단 (점수 추가 안 함)
        if (this.level >= MAX_LEVEL) {
            return 0;
        }

        this.score += amount;
        int earnedInk = 0; // 이번에 획득할 잉크 (레벨업 횟수)

        // 점수가 5 이상이면 레벨업 (반복문 처리: 한 번에 10점이 들어오면 2업 가능)
        while (this.score >= SCORE_FOR_LEVEL_UP) {
            // 만렙 도달 시 탈출
            if (this.level >= MAX_LEVEL) {
                this.score = 0; // 만렙 찍으면 잔여 점수 초기화
                break;
            }

            this.level++;
            this.score -= SCORE_FOR_LEVEL_UP;
            earnedInk++; // 레벨업 1회당 잉크 1개
        }

        return earnedInk; // 획득한 잉크 개수 반환
    }

    // =======================================================
    // [View Logic] 레벨 구간별 색상 코드 반환
    // (DB 저장 X, 조회 시 계산)
    // =======================================================
    public String getColor() {
        if (this.level >= 300) return "Violet"; // 보라 - 만렙
        if (this.level >= 251) return "Indigo"; // 남색
        if (this.level >= 201) return "Blue"; // 파랑
        if (this.level >= 151) return "Green"; // 초록
        if (this.level >= 101) return "Yellow"; // 노랑
        if (this.level >= 51)  return "Orange"; // 주황
        return "Red";                        // 빨강- 기본
    }
}