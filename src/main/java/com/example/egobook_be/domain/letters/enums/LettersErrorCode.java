package com.example.egobook_be.domain.letters.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LettersErrorCode implements BaseErrorCode {

    // 400
    REPLY_TEXT_EMPTY(HttpStatus.BAD_REQUEST, "PLAZA400_REPLY_TEXT_EMPTY", "답장 내용을 입력해주세요"),
    REPLY_TEXT_LIMIT(HttpStatus.BAD_REQUEST, "PLAZA400_REPLY_TEXT_LIMIT", "답장은 350자 이하여야 해요"),
    AI_MODERATION_FAILED(HttpStatus.BAD_REQUEST, "PLAZA400_AI_MODERATION_FAILED", "비속어/모욕적으로 표현된 의심 문장이 있어요"),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "PLAZA403_FORBIDDEN", "접근 권한이 없어요"),

    // 404
    LETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAZA404_LETTER_NOT_FOUND", "편지를 찾을 수 없어요"),

    // 409
    ALREADY_REPLIED(HttpStatus.CONFLICT, "PLAZA409_ALREADY_REPLIED", "이미 답장한 편지예요"),
    ALREADY_GAVE_UP(HttpStatus.CONFLICT, "PLAZA409_ALREADY_GAVE_UP", "24시간이 지나 답장할 수 없어요");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

