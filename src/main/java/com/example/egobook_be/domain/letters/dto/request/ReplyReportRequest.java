package com.example.egobook_be.domain.letters.dto.request;

import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ReplyReportRequest {

    @Schema(description = "답장 신고 사유", example = "ABUSE", allowableValues = "ABUSE, SPAM, INAPPROPRIATE, OTHER")
    private ReplyReportReason reason;

    @Schema(description = "기타 사유(선택)", example = "욕설이 포함된 답변")
    @Size(max = 500)
    private String description;
}