package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 탈퇴 이유 저장 요청 DTO")
public record WithdrawReasonReqDto(
        @Schema(description = "탈퇴 이유 유형",
                example = "NOT_USED_OFTEN",
                allowableValues = {"NOT_USED_OFTEN", "LACK_OF_CONTENT", "INCONVENIENT_UI", "DIFFICULT_TO_COLLECT_INK", "OTHER"})
        @NotNull(message = "탈퇴 이유 유형은 필수 입력 값입니다.")
        WithdrawReasonType reasonType,

        @Schema(description = "상세 탈퇴 이유 (기타 선택 시 필수 입력)",
                example = "앱의 디자인이 전반적으로 제 취향이 아니어서 떠납니다.",
                maxLength = 500)
        @Size(max = 500, message = "상세 이유는 최대 500자까지 입력 가능합니다.")
        String text
) {
}