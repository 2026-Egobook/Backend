package com.example.egobook_be.domain.letters.dto.request;

import com.example.egobook_be.global.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LetterReportRequest {

    @Schema(description = "편지 신고 사유", example = "ABUSE", allowableValues = "ABUSE, SPAM, INAPPROPRIATE, OTHER")
    private ReportReason reason;

    @Schema(description = "...")
    @Size(max = 500)
    private String description;
}
