package com.example.egobook_be.domain.diary.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiaryErrorCode implements BaseErrorCode {

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

    DIARY_EMOTION_LEVEL_NOT_ALLOWED(
            "DIARY_400_005",
            "감정 레벨을 선택할 수 없습니다.",
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

    DIARY_ACCESS_DENIED(
            "DIARY_403_001",
            "해당 일기에 접근할 권한이 없습니다.",
            HttpStatus.FORBIDDEN
    ),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}