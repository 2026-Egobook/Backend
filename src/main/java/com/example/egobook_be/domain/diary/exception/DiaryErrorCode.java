package com.example.egobook_be.domain.diary.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiaryErrorCode implements BaseErrorCode {

    DIARY_EMOTION_LEVEL_REQUIRED(
            "DIARY_400_001",
            "EMOTION 타입 선택 시 감정 레벨이 필요합니다.",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_EMOTION_LEVEL_INVALID(
            "DIARY_400_002",
            "감정 레벨은 1부터 5 사이여야 합니다.",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_TEXT_LIMIT_EXCEEDED(
            "DIARY_400_003",
            "일기는 최대 400자까지 입력할 수 있습니다.",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_DAILY_LIMIT_EXCEEDED(
            "DIARY_400_004",
            "하루에 최대 48번 기록 가능해요",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_EMOTION_LEVEL_NOT_ALLOWED(
            "DIARY_400_005",
            "감정 레벨을 선택할 수 없습니다.",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_TYPE_REQUIRED(
            "DIARY_400_006",
            "일기 유형을 최소 1개 이상 선택해야 합니다.",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_ACCESS_DENIED(
            "DIARY_403_001",
            "해당 일기에 접근할 권한이 없습니다.",
            HttpStatus.FORBIDDEN
    ),

    USER_NOT_FOUND(
            "DIARY_404_001",
            "해당 사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),

    DIARY_NOT_FOUND(
            "DIARY_404_002",
            "일기를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),

    EXPORT_FUTURE_DATE_NOT_ALLOWED(
            "DIARY_EXPORT_400_001",
            "미래 날짜는 내보낼 수 없어요",
            HttpStatus.BAD_REQUEST
    ),

    EXPORT_INVALID_DATE_RANGE(
            "DIARY_EXPORT_400_002",
            "시작 날짜가 끝 날짜보다 이전이거나 같아야 해요",
            HttpStatus.BAD_REQUEST
    ),

    EXPORT_RANGE_EXCEEDS_ONE_YEAR(
            "DIARY_EXPORT_400_003",
            "최대 1년 단위로 끊어서 내보낼 수 있어요",
            HttpStatus.BAD_REQUEST
    ),

    DIARY_EXPORT_FAILED(
            "DIARY_EXPORT_400_004",
            "파일 업로드에 실패했습니다",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    NO_DIARY_TO_EXPORT(
            "DIARY_EXPORT_404_001",
            "내보낼 수 있는 감정 일기가 없어요",
            HttpStatus.NOT_FOUND
    ),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}