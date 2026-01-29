package com.example.egobook_be.domain.terms.dto;

import com.example.egobook_be.domain.terms.enums.TermType;
import com.example.egobook_be.domain.terms.enums.TermVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "약관 상세 정보 응답 DTO")
public class TermResDto {
    @Schema(description = "약관 고유 ID", example = "1")
    private Long id;

    @Schema(description = "약관 타입 (서비스 이용약관, 개인정보 처리방침 등)", example = "SERVICE_USE")
    private TermType termType;

    @Schema(description = "약관 버전", example = "V1")
    private TermVersion termVersion;

    @Schema(description = "약관 제목(설명)", example = "서비스 이용 약관")
    private String description;

    @Schema(description = "약관 본문 (HTML 또는 Markdown)", example = "제1조 (목적)...")
    private String context;

    @Schema(description = "필수 동의 여부", example = "true")
    private boolean required;
}
