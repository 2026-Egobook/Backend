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

    public void update(String title, String notionUrl, LocalDateTime publishedAt) {
        this.title = title;
        this.notionUrl = notionUrl;
        this.publishedAt = publishedAt;
    }
}
