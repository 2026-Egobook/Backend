package com.example.egobook_be.domain.letters.entity;

import com.example.egobook_be.domain.letters.enums.LetterReportReason;
import com.example.egobook_be.global.entity.BaseReportEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PlazaLetterReport extends BaseReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PlazaLetter letter;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    private Long senderId;

    @Enumerated(EnumType.STRING)
    private LetterReportReason reason;
}