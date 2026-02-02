package com.example.egobook_be.domain.ego_room.dto;

import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import io.swagger.v3.oas.annotations.media.Schema;

public record CounselToneResDto(
        @Schema(description = "변경 된 상담 톤", example = "OBJECTIVE")
        CounselTone tone
) {
}