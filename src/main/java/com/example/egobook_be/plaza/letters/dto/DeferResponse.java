package com.example.egobook_be.plaza.letters.dto;

import com.example.egobook_be.plaza.letters.domain.PlazaLetterStatus;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeferResponse {
    private Long letterId;
    private PlazaLetterStatus status; // "DEFERRED"
}

