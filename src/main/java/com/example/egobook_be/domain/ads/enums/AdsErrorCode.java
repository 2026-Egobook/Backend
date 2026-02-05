package com.example.egobook_be.domain.ads.enums;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdsErrorCode implements BaseErrorCode {
    /* 500 */
    CLOUDINARY_VIDEO_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary에 Video를 업로드하던 중 알 수 없는 오류로 인해 업로드를 실패하였습니다.");

    private final HttpStatus status;
    private final String message;
}
