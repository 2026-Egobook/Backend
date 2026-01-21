package com.example.egobook_be.domain.diary.controller;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Tag(name = "Diary Controller", description = "감정 일기 관련 API")
public interface DiaryControllerDocs {

    @Operation(summary = "감정 일기 생성", description = """
            감정/고민/칭찬/감사 타입을 선택하여 일기를 작성합니다. (중복 선택 가능)
            
            - '감정' 선택 시, 감정 단계를 선택해야 합니다.
            - 텍스트 최대 400자 작성 가능합니다.
            - 일기 저장 날짜 변경 가능합니다.
            - 하루 최대 48번 일기 저장 가능합니다. (기준: 저장 날짜)
            - 리워드 규칙 (기준: 실제 작성 날짜)
              1. 오늘 최초 일기 작성 시, 잉크 +1
              2. 오늘 최초 '고민' 일기 작성 시, 감정 조절 +1
              3. 오늘 최초 '칭찬' 또는 '감사' 일기 작성 시, 긍정적 사고 +1
            """)
    @PostMapping
    ResponseEntity<GlobalResponse<DiaryCreateResDto>> createDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid DiaryCreateReqDto dto
    );

    @Operation(summary = "작성된 감정 일기 목록", description = """
            선택한 날짜에 작성된 감정 일기를 확인합니다.
            
            - 날짜를 반드시 선택해야 합니다.
            - 감정/고민/칭찬/감사 타입을 선택하여 해당 감정 일기를 확인합니다.
            - 일기 타입을 선택하지 않으면, 전체 감정 일기를 확인합니다.
            - 해당 날짜에 작성된 전체 일기 개수(dailyCount)를 반환합니다.(타입 선택 상관없이)
            """)
    @GetMapping
    ResponseEntity<GlobalResponse<DiaryListResDto>> getDiaries(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) DiaryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            );

    @Operation(summary = "작성된 감정 일기 상세 확인", description = "선택한 감정 일기의 상세 정보를 확인합니다.")
    @GetMapping("/{diaryId}")
    ResponseEntity<GlobalResponse<DiaryResDto>> getDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    );

    @Operation(summary = "감정 일기 수정", description = """
            작성된 감정 일기의 내용을 수정합니다.
            
            - 날짜를 제외한 일기 타입, 텍스트 내용을 수정 가능합니다.
            - '감정' 선택 시, 감정 단계를 선택해야 합니다.
            - 텍스트 최대 400자 작성 가능합니다.
            """)
    @PatchMapping("/{diaryId}")
    ResponseEntity<GlobalResponse<DiaryResDto>> updateDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId,
            @RequestBody @Valid DiaryUpdateReqDto dto
    );

    @Operation(summary = "감정 일기 삭제", description = "선택한 감정 일기를 영구 삭제합니다. (복구 불가)")
    @DeleteMapping("/{diaryId}")
    ResponseEntity<GlobalResponse<Boolean>> deleteDiary(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @PathVariable Long diaryId
    );

    @Operation(summary = "감정 일기 달력", description = """
            선택한 연도/월의 날짜별로 가장 많이 기록된 대표 감정 단계(이모티콘)를 확인합니다.
            
            - 날짜마다 하나의 감정 단계만 반환합니다.
            - 가장 많이 기록된 감정 단계가 여러 개일 경우, 가장 최근에 등록된 감정을 우선순위로 합니다.
            """)
    @GetMapping("/calendar")
    ResponseEntity<GlobalResponse<DiaryCalendarResDto>> getDiaryCalendar(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    );

    @Operation(summary = "감정 일기 내보내기", description = """
            PDF 또는 TEXT로 일기를 내보낼 수 있습니다.
            
            - 한 번에 최대 1년 단위로 선택 가능합니다.
            - 미래 날짜는 선택할 수 없습니다.
            - 시작일이 종료일보다 늦을 수 없습니다.
            """)
    @PostMapping("/export")
    ResponseEntity<GlobalResponse<DiaryExportResDto>> exportDiaries(
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,
            @RequestBody @Valid DiaryExportReqDto dto
    );
}
