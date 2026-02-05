package com.example.egobook_be.domain.ads.dto;

import lombok.Builder;

/** Cloudinary에 영상을 업로드한 뒤의 결과값 */
@Builder
public record CloudinaryUploadResult(
        Double videoDurationSec,
        String public_id
) {
}
