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
    LETTER_TEXT_LIMIT(HttpStatus.BAD_REQUEST, "PLAZA400_LETTER_TEXT_LIMIT", "편지는 360자 이하여야 해요"),
    DAILY_LETTER_LIMIT(HttpStatus.BAD_REQUEST, "PLAZA400_DAILY_LETTER_LIMIT", "하루에 한 번만 편지를 보낼 수 있어요"),
    INVALID_MODE(HttpStatus.BAD_REQUEST, "PLAZA400_INVALID_MODE", "mode 값이 올바르지 않아요"),
    FRIEND_ID_REQUIRED(HttpStatus.BAD_REQUEST, "PLAZA400_FRIEND_ID_REQUIRED", "FRIEND 모드에서는 toFriendId가 필요해요"),
    INVALID_REPORT_REASON(HttpStatus.BAD_REQUEST, "PLAZA400_INVALID_REPORT_REASON", "잘못된 신고 사유입니다."),
    ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "PLAZA400_ALREADY_REPORTED", "이미 신고한 답장입니다."),




    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "PLAZA403_FORBIDDEN", "접근 권한이 없어요"),


    // 404
    LETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAZA404_LETTER_NOT_FOUND", "편지를 찾을 수 없어요"),
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAZA404_THREAD_NOT_FOUND", "스레드를 찾을 수 없어요"),
    NO_RECEIVER_AVAILABLE(HttpStatus.NOT_FOUND,"PLAZA404_NO_RECEIVER_AVAILABLE", "현재 편지를 받을 수 있는 유저가 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"PLAZA404_USER_NOT_FOUND", "유저를 찾을 수 없어요."),


    // 409
    ALREADY_REPLIED(HttpStatus.CONFLICT, "PLAZA409_ALREADY_REPLIED", "이미 답장한 편지예요"),
    ALREADY_GAVE_UP(HttpStatus.CONFLICT, "PLAZA409_ALREADY_GAVE_UP", "24시간이 지나 답장할 수 없어요"),
    REPORT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "PLAZA409_REPORT_ALREADY_RESOLVED", "이미 해결된 신고입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

