package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder public class UserStats { @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 계산된 통계 데이터를 문자열(JSON)이나 핵심 필드로 저장
    @Column(columnDefinition = "TEXT")
    private String statsData;

    private LocalDateTime updatedAt;

    public void updateStats(String statsData) {
        this.statsData = statsData;
        this.updatedAt = LocalDateTime.now();
    }
}