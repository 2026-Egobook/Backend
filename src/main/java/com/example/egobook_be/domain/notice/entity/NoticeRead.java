package com.example.egobook_be.domain.notice.entity;

import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 유저별 공지 읽음 여부를 기록하는 엔티티. row가 없으면 안 읽음으로 간주한다.
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "notice_read",
        uniqueConstraints = @UniqueConstraint(name = "uk_notice_read_user_notice", columnNames = {"user_id", "notice_id"})
)
public class NoticeRead extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;
}
