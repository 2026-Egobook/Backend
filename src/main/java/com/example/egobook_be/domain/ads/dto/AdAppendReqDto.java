package com.example.egobook_be.domain.ads.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
public class AdAppendReqDto {
    // ========================================================
    // [ 1. 영상 파일 ]
    // ========================================================
    @Schema(description = "광고 영상 파일 (MP4, MOV)", type = "string", format = "binary")
    @NotNull(message = "광고 영상 파일은 필수입니다.")
    private MultipartFile videoFile;

    // ========================================================
    // [ 2. 광고주 정보 ]
    // ========================================================
    @Schema(description = "광고주 이름", example = "(주)에고컴퍼니")
    @NotBlank(message = "광고주 이름은 필수입니다.")
    private String advertiserName;

    @Schema(description = "광고주 이메일 (정산/알림용)", example = "contact@egocompany.com")
    @NotBlank(message = "광고주 이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String advertiserEmail;

    // ========================================================
    // [ 3. 광고 노출 정보 ]
    // ========================================================
    @Schema(description = "광고 제목 (사용자 노출용)", example = "신규 RPG 게임 사전예약")
    @NotBlank(message = "광고 제목은 필수입니다.")
    private String title;

    @Schema(description = "광고 상세 설명", example = "지금 사전예약하면 한정판 아이템 지급!")
    private String description; // Optional

    @Schema(description = "버튼 텍스트 (Call To Action)", example = "사전예약 하기")
    private String ctaText; // Optional (Default 처리 가능)

    @Schema(description = "클릭 시 이동할 랜딩 URL", example = "https://play.google.com/store/apps/details?id=...")
    @NotBlank(message = "랜딩 URL은 필수입니다.")
    private String landingUrl;

    // ========================================================
    // [ 4. 예산 및 보상 설정 ]
    // ========================================================
    @Schema(description = "총 광고 예산 (Integer)", example = "1000000")
    @NotNull(message = "총 예산은 필수입니다.")
    @Min(value = 1000, message = "최소 예산은 1,000원 이상이어야 합니다.")
    private Integer totalBudget; // 해당 값은 Ads의 remainingBudget에도 동일한 값이 들어간다.

    @Schema(description = "1회 시청 당 광고주 차감 비용 (CPV)", example = "100")
    @NotNull(message = "시청 단가는 필수입니다.")
    @Min(value = 1, message = "시청 단가는 1원 이상이어야 합니다.")
    private Integer costPerView;

    @Schema(description = "사용자에게 지급할 잉크 양", example = "10")
    @NotNull(message = "보상 잉크 양은 필수입니다.")
    @Min(value = 0, message = "보상 잉크는 0 이상이어야 합니다.")
    private Integer rewardInk;

    @Schema(description = "보상 지급 기준 시청 시간(초) (미입력 시 기본값 15초)", example = "15")
    private Double rewardGrantSec;

    // ========================================================
    // [ 5. 스케줄링 ]
    // ========================================================
    @Schema(description = "광고 시작 일시 (yyyy-MM-dd'T'HH:mm:ss)", example = "2024-05-01T00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // Form 데이터로 날짜 받을 때 포맷 지정 권장
    private LocalDateTime startAt;

    @Schema(description = "광고 종료 일시 (yyyy-MM-dd'T'HH:mm:ss)", example = "2024-05-31T23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;
}
