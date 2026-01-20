package com.example.egobook_be.infra.s3;


import com.example.egobook_be.global.enums.GlobalResponseCode;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Image API", description = "S3 이미지 업로드 관련 API") // 1. API 그룹 이름 설정
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageApi {

    // S3ImageService 클래스 가져옴(실제 업로드, 다운로드 등의 작업을 수행해주는 서비스 클래스)
    private final S3ImageService s3ImageService;

    @Operation(summary = "이미지 업로드", description = "이미지 파일을 S3에 업로드하고 URL을 반환합니다.")
    /**
     * consumes:
     * - "소비하다"라는 뜻.클라이언트가 보내는 데이터의 "타입(MIME Type)"을 제한하는 필터
     * - MediaType.MULTIPART_FORM_DATA_VALUE: 문자열 "multipart/form-data"를 의미
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<?>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일 (.jpg, .png 등)") // 3. 파라미터 설명 추가
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        String imageUrl = s3ImageService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "이미지를 성공적으로 S3에 업로드하였습니다.", imageUrl));
    }
}