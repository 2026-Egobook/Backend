package com.example.egobook_be.global.entity;

import com.example.egobook_be.global.enums.ReportStatus;
import jakarta.persistence.*;
        import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(force = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseReportEntity {

    @Column(length = 500)
    private String description;         // 신고 상세 설명

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 자동 주입

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;        // 공통 신고 상태

    // reporterId는 도메인마다 타입이 달라서 각 자식에서 선언
}