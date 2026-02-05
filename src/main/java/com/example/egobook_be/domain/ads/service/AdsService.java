package com.example.egobook_be.domain.ads.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.egobook_be.domain.ads.dto.AdAppendReqDto;
import com.example.egobook_be.domain.ads.dto.AdAppendResDto;
import com.example.egobook_be.domain.ads.dto.CloudinaryUploadResult;
import com.example.egobook_be.domain.ads.entity.Ads;
import com.example.egobook_be.domain.ads.enums.AdsErrorCode;
import com.example.egobook_be.domain.ads.mapper.AdsMapper;
import com.example.egobook_be.domain.ads.repository.AdsRepository;
import com.example.egobook_be.global.exception.CustomException;
import io.netty.util.internal.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdsService {
    private final Cloudinary cloudinary;
    private final AdsRepository adsRepository;
    private final AdsMapper adsMapper;


    /**
     * 광고 영상을 Cloudinary에 저장하고, 해당 영상의 메타데이터를 Ads Table에 저장하는 함수
     */
    @Transactional
    public AdAppendResDto appendAds(AdAppendReqDto reqDto){
        // 1. 영상을 Cloudinary에 업로드하고, 메타데이터를 획득합니다.
        CloudinaryUploadResult uploadResult;
        try {
            uploadResult = uploadAdVideo(reqDto.getVideoFile());
        } catch (IOException e) {
            log.error("Cloudinary Upload Failed", e);
            throw new CustomException(AdsErrorCode.CLOUDINARY_VIDEO_UPLOAD_ERROR);
        }

        // 2. Ads Entity build
        Ads newAd = adsMapper.toEntity(reqDto, uploadResult);

        // 3. DB 저장
        Ads savedAd = adsRepository.save(newAd);

        // 4. 응답 DTO 변환
        return adsMapper.toAdAppendResDto(savedAd);
    }


    /**
     * 비디오 파일을 Cloudinary에 업로드한 후, 해당 파일의 정보들을 받아오는 함수
     * [ 반환 데이터 ]
     *
     * */
    private CloudinaryUploadResult uploadAdVideo(MultipartFile file) throws IOException {
        // 1. 파일 업로드 설정
        Map params = ObjectUtils.asMap(
                "resource_type", "video", // (1) 파일 타입 명시
                "folder", "ads/reward_videos", // (2) 클라우드 내 폴더 경로 설정
                "public_id", "ad_"+System.currentTimeMillis(), // (3) 패당 파일의 public_id 설정
                /*
                 * (4) EagerTransformation 설정 (사전 변환 설정)
                 * - 보통 클라우드 이미지/비디오 서비스는 사용자가 url을 호출할 때 변환을 수행하는 lazy 변환이 기본이다.
                 * - 비디오는 그렇게 하면 용량이 크므로 변환에 시간이 오래 걸리므로, "파일이 업로드 되는 즉시 아래에 지정한 포맷(HLS, HD 화질)으로 미리 변환해서 저장해두는 설정이다.
                 */
                "eager", Arrays.asList(
                        new EagerTransformation().streamingProfile("hd"), // HD 화질 스트리밍 설정
                        new EagerTransformation().format("m3u8")                // HLS 포맷 설정
                ),
                "eager_async", true // (5) 업로드 응답을 기다리지 않고, 백그라운드에서 변환
                // "eager_notification_url", "https://mysite.example.com/notify_endpoint") // (6) 작업이 완료되면 특정 url로
        );
        // 2. 실제 업로드 수행
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        // 3. 결과에서 "public_id"를 리턴 (DB 저장 용)
        return CloudinaryUploadResult.builder()
                .public_id((String) uploadResult.get("public_id"))
                .videoDurationSec((Double) uploadResult.get("duration"))
                .build();
    }
}
