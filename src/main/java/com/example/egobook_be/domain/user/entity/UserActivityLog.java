package com.example.egobook_be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "user_activity_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_activity", columnNames = {"user_id", "active_date"})
        },
        indexes = {
                @Index(name = "idx_active_date", columnList = "active_date"),
                @Index(name = "idx_user_activity", columnList = "user_id, active_date")
        }
)
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "active_date", nullable = false)
    private LocalDate activeDate;
}