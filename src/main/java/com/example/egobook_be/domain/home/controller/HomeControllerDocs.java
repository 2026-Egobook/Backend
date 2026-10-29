package com.example.egobook_be.domain.home.controller;

import com.example.egobook_be.domain.home.dto.HomeAbilityResDto;
import com.example.egobook_be.domain.home.dto.HomeActivityResDto;
import com.example.egobook_be.domain.home.dto.HomeResDto;
import com.example.egobook_be.domain.home.dto.HomeSettingResDto;
import com.example.egobook_be.domain.home.dto.TurtleMessageResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Home Controller", description = "홈(메인) 화면 관련 API")
@RequestMapping("/home")
public interface HomeControllerDocs {
    @Operation(summary = "홈 화면 정보 조회", description = """
            앱 실행 후 메인(홈) 화면에 진입할 때 필요한 모든 정보를 한 번에 조회합니다.
            
            - **반환 데이터**:
              1. ```nickname```: 사용자 닉네임
              2. ```level```: 사용자 현재 레벨
              3. ```ink```: 사용자 현재 보유 잉크
              4. ```unreadNotifications```: 사용자가 아직 읽지 않은 알림 개수(Red Dot)
              5. ```hasUnopenedPsychology```: 사용자가 아직 열지 않은 오늘의 심리 지식 여부
              6. ```isFirstAttendanceToday```: 오늘 최초 출석인지 여부
              7. ```attendanceRewardInk```: 최초 출석시 보상 잉크 값
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeResDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    ResponseEntity<GlobalResponse<HomeResDto>> getHomeData(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(summary = "활동 목록 정보 조회", description = """
            홈 화면의 '활동 목록' 탭에 필요한 데이터를 조회합니다.
            
            - **반환 데이터**:
              1. ```isDailyMissionSuccess```: 하루 미션(일기/편지/질문) 수행 완료 여부
              2. ```hasWrittenDiary```: 감정일기 작성 여부
              3. ```hasWrittenLetter```: 편지 작성 여부
              4. ```hasAnsweredQuestion```: 오늘의 질문 답변 여부
              5. ```consecutiveWeeks```: 연속 수행 주차 (N주차, 첫 시작은 1주차부터 시작입니다!)
              6. ```weeklyMissionStatus```: 이번 주(월~일) 미션 달성 여부 배열
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = HomeActivityResDto.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/activities")
    ResponseEntity<GlobalResponse<HomeActivityResDto>> getHomeActivities(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(summary = "사용자 레이더 차트 정보 조회", description = """
            홈 화면의 '능력치(레이더 차트)' 탭에 필요한 데이터를 조회합니다.
            5가지 능력치(공감성, 자존감, 성실함, 긍정사고, 감정조절)에 대한 레벨, 점수를 반환합니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = HomeAbilityResDto.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/abilities")
    ResponseEntity<GlobalResponse<HomeAbilityResDto>> getHomeAbilities(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(summary = "사용자 설정/계정 페이지 정보 조회", description = """
            홈 화면의 '설정(계정)' 탭에 필요한 데이터를 조회합니다.
            
            [ 반환 데이터 ]
            1. 사용자 계정 ID(Account Code)
            2. 사용자 이메일 (Google 연동 했을 시에만 반환됩니다)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = HomeSettingResDto.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/settings")
    ResponseEntity<GlobalResponse<HomeSettingResDto>> getHomeSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );

    @Operation(summary = "고북이 말풍선 문구 조회", description = """
            메인 화면에서 고북이를 클릭했을 때 말풍선에 노출할 문구 **1개**를 조회합니다.

            - **호출할 때마다** 서버가 등록된 문구 중 하나를 무작위로 골라 내려줍니다.
              (고북이를 클릭할 때마다 이 API를 호출하면 됩니다.)
            - 같은 문구가 연속으로 나올 수 있습니다.
            - 말풍선 연타 시 겹침 방지, 약 3초 후 자동 사라짐은 클라이언트 동작입니다.

            [ 반환 데이터 ]
            1. ```message```: 말풍선에 표시할 문구 (등록된 문구가 없으면 ```null```이므로, null이면 말풍선을 표시하지 않아야 합니다)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TurtleMessageResDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/turtle-message")
    ResponseEntity<GlobalResponse<TurtleMessageResDto>> getTurtleMessage();
}
