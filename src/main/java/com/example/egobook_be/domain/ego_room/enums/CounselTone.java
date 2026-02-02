package com.example.egobook_be.domain.ego_room.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CounselTone {
    SHARP("날카로움"),
    SOFT("부드러움"),
    OBJECTIVE("객관적");

    private final String description;
}