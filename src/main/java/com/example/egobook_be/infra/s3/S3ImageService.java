package com.example.egobook_be.infra.s3;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final S3Template s3Template; // Spring Cloud AWS가 제공하는 핵심 유틸리티

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName; // yml에서 버킷 이름 가져오기

    public String upload(MultipartFile image) throws IOException{
        // 1. 빈 파일 체크
        if (image.isEmpty() || image.getOriginalFilename() == null) {
            throw new IllegalArgumentException("이미지 파일이 존재하지 않습니다.");
        }

        // 2. 유니크한 파일명 생성 (충돌 방지)
        // 예: original.jpg -> 550e8400-e29b...jpg
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + originalFilename;

        // 3. S3에 업로드 (핵심 로직)
        try (InputStream inputStream = image.getInputStream()) {
            // s3Template이 알아서 Content-Type 설정 및 업로드를 처리합니다.
            s3Template.upload(bucketName, s3FileName, inputStream);
        } catch (IOException e) {
            log.error("S3 업로드 중 에러 발생", e);
            throw new RuntimeException("이미지 업로드에 실패했습니다.");
        }

        // 4. 업로드된 이미지의 URL 반환
        // 주의: 버킷이 Private인 경우 이 URL로 바로 접근이 안 될 수 있습니다. (Presigned URL 등 필요)
        return s3Template.download(bucketName, s3FileName).getURL().toString();
    }
}
