package com.example.egobook_be.domain.notification.entity;

import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification")
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 17)
    private String content;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void markAsRead() {
        this.isRead = true;
    }
}
