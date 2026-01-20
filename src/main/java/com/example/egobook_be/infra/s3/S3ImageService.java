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

    /**
     * S3Template:
     *  - 복잡한 aws S3 SDK를 Spring 개발자가 쓰기 편하게 포장해놓은 도구
     *  - 과거엔 AmazonS3Client를 직접 사용했지만, 최신 버전에서는 해당 S3와의 통신
     *  - (업로드, 다운로드, 삭제 등)을 훨씬 쉽고 안전하게 대행해준다.
     */
    private final S3Template s3Template;

    // @Value를 이용해 yml에서 버킷 이름 가져오기
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public String upload(MultipartFile image) throws IOException{
        /**
         * 1. 빈 파일 체크
         * - 함수 인자로 받은 MultipartFile 객체의 유효성을 검증한다.
         * - 사용자가 파일을 선택하지 않고 업로드 버튼을 눌렀거나, 파일명이 깨진 경우를 방어한다.
         * - getOriginalFilename()이 null인 경우도 확인한다.
         */
        if (image.isEmpty() || image.getOriginalFilename() == null) {
            throw new IllegalArgumentException("이미지 파일이 존재하지 않습니다.");
        }

        /**
         * 2. 파일명 중복 방지를 위한 유니크한 파일명 생성
         * - S3는 같은 이름의 파일이 올라오면 덮어씌워버린다.
         * - 따라서, UUID(난수, 랜덤 문자열)을 생성해서 파일명 앞에 붙여준다. (ex: a1b2c3d4e5_profile.jpg)
         */
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + originalFilename;

        /**
         * 3. S3에 업로드
         * [ s3Template.upload(bucket, key, stream); ]
         * - InputStream 사용. 이전 방식은 파일을 서버 디스크에 저장(File)했다가 올리고 지웠는데,
         * 이 코드는 메모리 상의 데이터 흐름(InputStream)을 S3로 곧바로 쏘아보낸다.
         */
        try (InputStream inputStream = image.getInputStream()) {
            // s3Template이 알아서 Content-Type 설정 및 업로드를 처리합니다.
            s3Template.upload(bucketName, s3FileName, inputStream);
        } catch (IOException e) {
            log.error("S3 업로드 중 에러 발생", e);
            throw new RuntimeException("이미지 업로드에 실패했습니다.");
        }

        /**
         * 4. 업로드된 이미지의 URL 반환
         * [ S3Resource file = s3Template.download(bucket, key); ]
         * - 주의: 버킷이 Private인 경우 이 URL로 바로 접근이 안 될 수 있습니다. (Presigned URL 등 필요)
         */
        return s3Template.download(bucketName, s3FileName).getURL().toString();
    }
}
