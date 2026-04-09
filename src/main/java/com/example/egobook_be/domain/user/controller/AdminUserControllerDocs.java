package com.example.egobook_be.domain.user.controller;

import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserReportHistoryResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.enums.ReportType;
import com.example.egobook_be.global.response.GlobalResponse;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin User Controller", description = "회원관리 관리자 API")
@RequestMapping("/admin/users")
public interface AdminUserControllerDocs {
    @Operation(summary = "회원 리스트 검색", description = """
            키워드 & 필터 검색을 통해 회원들을 리스트로 검색하는 API입니다.
            
            [**Query Parameter**]
            - page: 페이지 번호 (1 ~ n)
            - size: 페이지 크기
            - keyword: 검색창에 작성한 검색 키워드
                (검색할 수 있는 요소= ```Account Code``` & ```Email``` & ```Nickname```)
            - status: 검색할 사용자의 상태 필터
                (필터링할 수 있는 요소)
                    1. ```ACTIVE``` (활동 상태)
                    2. ```DORMANT``` (휴면 상태)
                    3. ```WITHDRAW_PENDING``` (탈퇴 대기 상태)
                    4. ```SUSPENDED``` (정지 상태)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 리스트 검색 성공",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 필터 값을 보냈습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 키워드 및 필터에 맞는 사용자 정보들을 찾지 못했습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    ResponseEntity<GlobalResponse<SliceResponse<SearchUserResDto>>> searchUserList(
        @Parameter(description = "검색 키워드 (AccountCode | Email | Nickname", required = true)
        @RequestParam("keyword") String keyword,

        @Parameter(description = "사용자 상태 필터 키워드 (ACTIVE | DORMANT | WITHDRAW_PENDING | SUSPENDED)")
        @RequestParam("status") UserStatus status,

        @Parameter(description = "Page 번호 (1 ~ N)", required = true)
        @RequestParam(value = "page", defaultValue = "1") Integer page,

        @Parameter(description = "Page 크기", required = true)
        @RequestParam(value = "size", defaultValue = "5") Integer size
    );

    @Operation(summary = "회원 기본 정보 조회", description = """
            특정 회원의 기본 정보를 조회하는 API입니다.

            [**Path Variable**]
            - userId: 조회할 ```ROLE_USER``` 권한을 가진 사용자의 ID

            [**반환 정보**]
            - ```userId```: 사용자 PK
            - ```accountCode```: 사용자 계정 고유 코드
            - ```email```: 사용자 이메일 (구글 연동된 계정일 경우만 존재)
            - ```provider```: 가입 유형 (**GUEST** | **GOOGLE**)
            - ```nickname```: 사용자 닉네임
            - ```createdAt```: 계정 생성 일시
            - ```lastLoginAt```: 마지막으로 로그인한 일시
            - ```status```: 사용자 계정 상태 (**ACTIVE** | **DORMANT** | **WITHDRAW_PENDING** | **SUSPENDED**)
            - ```deletedAt```: 사용자가 탈퇴 신청한 일시 (SOFT DELETE 신청한 일시)
            - ```purgeAt```: 실제 사용자 데이터가 전부 삭제 예정인 일시
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 기본 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminUserInfoResDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}")
    ResponseEntity<GlobalResponse<AdminUserInfoResDto>> getUserInfo(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId
    );

    @Operation(summary = "회원 활동 통계 조회", description = """
            특정 회원의 활동 통계를 조회하는 API입니다.

            [**Path Variable**]
            - userId: 조회할 ```ROLE_USER``` 권한을 가진 사용자의 ID

            [**반환 정보**]
            - ```userId```: 사용자 PK
            - ```activityCount```: 카테고리별 활동 횟수
                - ```diary```: 일기 작성 횟수
                - ```letter```: 편지 작성 횟수
                - ```letterReply```: 편지 답변 횟수
                - ```questionAnswer```: 오늘의 질문 답변 횟수
            - ```abilityLevel```: 능력치별 레벨
                - ```empathy```: 공감성 레벨
                - ```selfEsteem```: 자존감 레벨
                - ```emotionRegulation```: 감정조절 레벨
                - ```positiveThinking```: 긍정적 사고 레벨
                - ```diligence```: 성실성 레벨
            - ```letterReceiveBlockedUntil```: 편지 수신 차단 종료 일시 (차단 없으면 ```null```)
            - ```notificationEnabled```: 알람 수신 여부 (```true```: 수신, ```false```: 비수신)
            - ```isFirstAttendanceToday```: 오늘 첫 출석 여부 (```true```: 첫 출석 안함, ```false```: 첫 출석 함)
            - ```weeklyAnalysisEnabled```: 주간 분석 보고서 수신 여부 (```true```: 수신, ```false```: 비수신)
            - ```counselingTone```: 주간 분석 말투 (**SHARP** | **SOFT** | **OBJECTIVE**)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 활동 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminUserStatsResDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}/stats")
    ResponseEntity<GlobalResponse<AdminUserStatsResDto>> getUserStats(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId
    );

    @Operation(summary = "회원 신고 이력 조회", description = """
        특정 회원의 신고 이력 및 제재 요약 정보를 조회하는 API입니다.

        [**Path Variable**]
        - userId: 조회할 ```ROLE_USER``` 권한을 가진 사용자의 ID

        [**Query Parameter**]
        **NOT NULL**
        - page: 페이지 번호 (1 ~ n)
        - size: 페이지 크기
        - reportDomainType: 신고 도메인 타입 필터
            (필터링할 수 있는 요소)
                1. ```LETTER``` (편지)
                2. ```LETTER_REPLY``` (편지 답장)
                3. ```QUESTION_ANSWER``` (오늘의 질문 답장)
        **NULL**
        - reportType: 신고 타입 필터
            (필터링할 수 있는 요소)
                1. ```REPORTER``` (신고한 내역)
                2. ```REPORTED``` (신고당한 내역)
        - reportReason: 신고 사유 필터
            (필터링할 수 있는 요소)
                1. ```ABUSE``` (비속어/욕설/모욕)
                2. ```SPAM``` (광고/스팸)
                3. ```INAPPROPRIATE``` (부적절한 콘텐츠)
                4. ```OTHER``` (기타)
        - reportStatus: 신고 처리 상태 필터
            (필터링할 수 있는 요소)
                1. ```PENDING``` (대기중)
                2. ```RESOLVED``` (처리 완료)
                3. ```REFUSED``` (처리 반려)

        [**반환 정보**]
        - ```userId```: 사용자 PK
        - ```summary```: 사용자 신고 관련 요약 정보 (REPORT_TYPE을 지정해주지 않으면 totalReportCount,totalReportedCount의 개수는 총합으로 나옵니다.) 
            - ```totalReportCount```: 지금까지 신고한 누적 횟수
            - ```totalReportedCount```: 지금까지 신고당한 누적 횟수
            - ```pastSuspendedCount```: 과거 계정 정지 횟수
        - ```reportList```: 신고 이력 페이지 정보
            - ```content```: 신고 이력 리스트
                - ```reportId```: 신고 PK (domainType별로 적용되는 엔티티가 달라짐)
                - ```reportDomainType```: 신고 도메인 타입 (**LETTER** | **LETTER_REPLY** | **QUESTION_ANSWER**)
                - ```reportType```: 신고 타입 (**REPORTER** | **REPORTED**)
                - ```reportReason```: 신고 사유 (**ABUSE** | **SPAM** | **INAPPROPRIATE** | **OTHER**)
                - ```reportStatus```: 신고 처리 상태 (**PENDING** | **RESOLVED** | **REFUSED**)
                - ```createdAt```: 신고 일자
                - ```targetId```: 신고 대상의 PK
                - ```content```: 신고 받은 내용
            - ```page```: 현재 페이지 번호
            - ```size```: 한 페이지 최대 개수
            - ```hasNext```: 다음 페이지 존재 여부
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 신고 이력 조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminUserReportHistoryResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 필터 값을 보냈습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없습니다.",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}/report-history")
    ResponseEntity<GlobalResponse<AdminUserReportHistoryResDto>> getUserReportHistory(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId,

            @Parameter(description = "신고 도메인 타입 필터 (LETTER | LETTER_REPLY | QUESTION_ANSWER)")
            @RequestParam(value = "reportDomainType", required = true) ReportDomainType reportDomainType,

            @Parameter(description = "신고 타입 필터 (REPORTER | REPORTED)")
            @RequestParam(value = "reportType", required = false) ReportType reportType,

            @Parameter(description = "신고 사유 필터 (ABUSE | SPAM | INAPPROPRIATE | OTHER)")
            @RequestParam(value = "reportReason", required = false) ReportReason reportReason,

            @Parameter(description = "신고 처리 상태 필터 (PENDING | RESOLVED | REFUSED)")
            @RequestParam(value = "reportStatus", required = false) ReportStatus reportStatus,

            @Parameter(description = "Page 번호 (1 ~ N)", required = true)
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "Page 크기", required = true)
            @RequestParam(value = "size", defaultValue = "10") Integer size
    );
}
