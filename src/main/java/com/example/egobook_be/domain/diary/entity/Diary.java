package com.example.egobook_be.domain.diary.entity;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "diary")
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private Integer emotionLevel;

    @Column(nullable = false, length = 400)
    private String content;

    /** 일기 Type (감정, 고민, 칭찬, 감사) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "diary_type", joinColumns = @JoinColumn(name = "diary_id"))
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Set<DiaryType> type = EnumSet.noneOf(DiaryType.class);
    private LocalDateTime writtenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void update(@NotBlank String content,
                       @NotEmpty Set<DiaryType> type,
                       @Max(5) @Min(1) Integer emotionLevel) {
        this.content = content;
        this.type = type;
        this.emotionLevel = emotionLevel;
        this.writtenAt = LocalDateTime.now();
    }
}
