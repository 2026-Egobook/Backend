package com.example.egobook_be.domain.restriction.mapper;

import com.example.egobook_be.domain.restriction.dto.RestrictionCancelResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.dto.MyRestrictionResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionInfoResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RestrictionMapper {

    // 자동 제재 팝업 표시 문구
    private static final String AUTO_REPORT_DISPLAY_REASON = "커뮤니티 이용 규칙 위반 (반복된 신고 접수)";

    /**
     * Restriction Entity -> RestrictionCreateResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @return 변환된 RestrictionCreateResDto
     */
    public RestrictionCreateResDto toResDto(Restriction restriction) {
        return RestrictionCreateResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .restrictionStatus(restriction.getStatus())
                .restrictionUntil(restriction.getRestrictionUntil())
                .build();
    }

    /**
     * Restriction Entity -> RestrictionItemResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @return 변환된 RestrictionItemResDto
     */
    public RestrictionItemResDto toItemResDto(Restriction restriction) {
        return RestrictionItemResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .domainType(restriction.getDomainType())
                .reason(restriction.getReason())
                .description(restriction.getDescription())
                .restrictionStatus(restriction.getStatus())
                .createdAt(restriction.getCreatedAt())
                .restrictionUntil(restriction.getRestrictionUntil())
                .build();
    }

    /**
     * Restriction Entity, User Entity -> RestrictionCancelResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @param user 변환할 User Entity
     * @return 변환된 RestrictionCancelResDto
     */
    // [AI-GEN] 제재 해제 응답 매핑
    public RestrictionCancelResDto toCancelResDto(Restriction restriction, User user) {
        return RestrictionCancelResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .restrictionStatus(restriction.getStatus())
                .userId(user.getId())
                .userStatus(user.getStatus())
                .restrictionUntil(null)
                .build();
    }

    /**
     * Restriction Entity -> RestrictionInfoResDto 변환
     * - 자동 제재 사유 코드(AUTO_REPORT_3)는 팝업 표시용 문구로 바꾸고, 관리자가 직접 입력한 사유는 그대로 사용한다.
     * @param restriction 변환할 ACTIVE 상태의 Restriction Entity
     * @return 제재 중(restricted=true)인 RestrictionInfoResDto
     */
    // [AI-GEN] 제재 중 상태 응답 매핑
    public RestrictionInfoResDto toInfoResDto(Restriction restriction) {
        return RestrictionInfoResDto.builder()
                .restricted(true)
                .reason(toDisplayReason(restriction.getReason()))
                .restrictedUntil(restriction.getRestrictionUntil())
                .build();
    }

    /**
     * 제재 중이 아닌 상태의 RestrictionInfoResDto 생성
     * @return restricted=false, reason/restrictedUntil=null 인 RestrictionInfoResDto
     */
    // [AI-GEN] 제재 없음 상태 응답 매핑
    public RestrictionInfoResDto toUnrestrictedInfoResDto() {
        return RestrictionInfoResDto.builder()
                .restricted(false)
                .build();
    }

    /**
     * 도메인별 제재 상태 -> MyRestrictionResDto 변환
     * @param letter         편지 기능 제재 상태
     * @param questionAnswer 오늘의 질문 답변 기능 제재 상태
     * @return 변환된 MyRestrictionResDto
     */
    // [AI-GEN] 내 제재 상태 응답 매핑
    public MyRestrictionResDto toMyRestrictionResDto(RestrictionInfoResDto letter, RestrictionInfoResDto questionAnswer) {
        return MyRestrictionResDto.builder()
                .letter(letter)
                .questionAnswer(questionAnswer)
                .build();
    }

    /**
     * 제재 사유를 팝업 표시용 문구로 변환
     * @param reason DB에 저장된 제재 사유
     * @return 자동 제재 코드면 표시 문구, 그 외에는 입력된 사유 그대로
     */
    private String toDisplayReason(String reason) {
        if (Restriction.AUTO_REPORT_3_REASON.equals(reason)) {
            return AUTO_REPORT_DISPLAY_REASON;
        }
        return reason;
    }
}
