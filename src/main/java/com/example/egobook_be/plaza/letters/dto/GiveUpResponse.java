package com.example.egobook_be.plaza.letters.dto;

import com.example.egobook_be.plaza.letters.domain.PlazaLetterStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GiveUpResponse {
    private Long letterId;
    private PlazaLetterStatus status;     // "GAVE_UP"
    private OffsetDateTime gaveUpAt;
}

