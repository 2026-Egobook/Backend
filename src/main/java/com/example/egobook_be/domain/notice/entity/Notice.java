package com.example.egobook_be.domain.notice.entity;

import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "notice")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "notion_url", nullable = false, length = 500)
    private String notionUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    // 발행 시각 도달 후 전체 유저 브로드캐스트가 이미 수행됐는지 여부 (스케줄러 중복 발송 방지)
    @Column(name = "broadcasted", nullable = false)
    @Builder.Default
    private boolean broadcasted = false;

    public void update(String title, String notionUrl, LocalDateTime publishedAt) {
        this.title = title;
        this.notionUrl = notionUrl;
        this.publishedAt = publishedAt;
    }

    public void markBroadcasted() {
        this.broadcasted = true;
    }
}
