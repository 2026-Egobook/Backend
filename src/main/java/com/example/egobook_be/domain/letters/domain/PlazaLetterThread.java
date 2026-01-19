package com.example.egobook_be.domain.letters.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "plaza_letter_threads")
public class PlazaLetterThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long threadId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static PlazaLetterThread createNow() {
        return PlazaLetterThread.builder()
                .createdAt(OffsetDateTime.now())
                .build();
    }
}