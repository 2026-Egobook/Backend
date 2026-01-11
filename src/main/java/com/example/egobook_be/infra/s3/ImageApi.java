package com.example.egobook_be.infra.s3;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    private final S3ImageService s3ImageService;

    @Operation(summary = "이미지 업로드", description = "이미지 파일을 S3에 업로드하고 URL을 반환합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 2. 핵심: 이게 있어야 '파일 선택' 버튼이 생김
    public ResponseEntity<String> uploadImage(
            @Parameter(description = "업로드할 이미지 파일 (.jpg, .png 등)") // 3. 파라미터 설명 추가
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        String imageUrl = s3ImageService.upload(file);
        return ResponseEntity.ok(imageUrl);
    }
}