package com.example.egobook_be.domain.letters.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

    public static PlazaLetterThread createNow() {
        return PlazaLetterThread.builder()
                .createdAt(LocalDateTime.now())
                .build();
    }
}