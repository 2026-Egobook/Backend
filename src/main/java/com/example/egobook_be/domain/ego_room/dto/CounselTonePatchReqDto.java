package com.example.egobook_be.domain.ego_room.dto;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;

public record CounselTonePatchReqDto(
        CounselTone toneStyle
) {
}