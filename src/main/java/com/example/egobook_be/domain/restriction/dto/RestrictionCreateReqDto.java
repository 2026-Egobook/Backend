package com.example.egobook_be.domain.restriction.dto;

import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.global.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestrictionCreateReqDto(

        @Schema(description = "제재 도메인 타입 (LETTER | QUESTION_ANSWER)", example = "LETTER")
        @NotNull(message = "제재 도메인 타입은 필수입니다.")
        RestrictionDomainType domainType,

        @Schema(description = "제재 사유 (ABUSE | SPAM | INAPPROPRIATE | OTHER)", example = "ABUSE")
        @NotNull(message = "제재 사유는 필수입니다.")
        ReportReason reason,

        @Schema(description = "제재 사유 설명", example = "반복적인 욕설 사용")
        @NotBlank(message = "제재 사유 설명은 필수입니다.")
        @Size(max = 500, message = "제재 사유 설명은 최대 500자까지 입력 가능합니다.")
        String description
) {}
