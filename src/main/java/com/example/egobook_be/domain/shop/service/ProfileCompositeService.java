package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.ProfileImageResDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileCompositeService {

    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private static final Map<ItemCategory, Integer> LAYER_ORDER = Map.of(
            ItemCategory.BACK,      0,
            ItemCategory.SKIN,      1,
            ItemCategory.DECOR_ONE, 2,
            ItemCategory.DECOR_TWO, 3
    );

    private static final int CANVAS_W = 304;
    private static final int CANVAS_H = 197;

    public ProfileImageResDto compositeAndUpload(Long userId) {

        List<UserItem> equippedItems = userItemRepository.findEquippedItems(userId)
                .stream()
                .filter(ui -> ui.getItem().getCategory() != ItemCategory.LETTER_PAPER)
                .sorted(Comparator.comparingInt(ui ->
                        LAYER_ORDER.getOrDefault(ui.getItem().getCategory(), 99)))
                .toList();

        if (equippedItems.isEmpty()) {
            log.info("[ProfileCompositeService] 장착 아이템 없음 - userId: {}", userId);
            return new ProfileImageResDto(null, null);
        }

        String backgroundImageUrl = equippedItems.stream()
                .filter(ui -> ui.getItem().getCategory() == ItemCategory.BACKGROUND)
                .findFirst()
                .map(ui ->
                        cloudfrontDomain + "/my/" + ui.getItem().getPath() + "/" + ui.getItem().getName()
                )
                .orElse(null);

        List<UserItem> turtleLayers = equippedItems.stream()
                .filter(ui -> LAYER_ORDER.containsKey(ui.getItem().getCategory()))
                .toList();

        String turtleImageUrl = null;

        if (!turtleLayers.isEmpty()) {
            BufferedImage canvas = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            try {
                for (UserItem ui : turtleLayers) {
                    Item item = ui.getItem();
                    String s3ItemKey = "files/my/" + item.getPath() + "/" + item.getName();

                    if ((item.getCategory() == ItemCategory.DECOR_ONE ||
                            item.getCategory() == ItemCategory.DECOR_TWO)
                            && item.getName().equals("Default.png")) {
                        continue;
                    }

                    try {
                        byte[] imageBytes = s3Client.getObjectAsBytes(
                                GetObjectRequest.builder().bucket(bucketName).key(s3ItemKey).build()
                        ).asByteArray();
                        BufferedImage layer = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        if (layer == null) {
                            log.warn("[ProfileCompositeService] 이미지 로드 실패 - key: {}", s3ItemKey);
                            continue;
                        }
                        g.drawImage(layer, 0, 0, CANVAS_W, CANVAS_H, null);
                    } catch (Exception e) {
                        log.warn("[ProfileCompositeService] 레이어 스킵 - key: {}, error: {}", s3ItemKey, e.getMessage());
                    }
                }
            } finally {
                g.dispose();
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(canvas, "png", baos);
                byte[] pngBytes = baos.toByteArray();

                long timestamp = System.currentTimeMillis();
                String s3Key = String.format("files/my/profile/%d_%d.png", userId, timestamp);
                try (ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes)) {
                    s3Template.upload(bucketName, s3Key, bais);
                }
                turtleImageUrl = cloudfrontDomain + String.format("/my/profile/%d_%d.png", userId, timestamp);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 생성 실패: " + e.getMessage(), e);
            }
        }

        return new ProfileImageResDto(turtleImageUrl, backgroundImageUrl);
    }

    public ProfileImageResDto confirmProfile(Long userId) {
        ProfileImageResDto result = compositeAndUpload(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (user.getTurtleImageUrl() != null) {
            try {
                String oldKey = "files/" + user.getTurtleImageUrl()
                        .replace(cloudfrontDomain + "/", "");
                s3Template.deleteObject(bucketName, oldKey);
            } catch (Exception e) {
                log.warn("[ProfileCompositeService] 기존 프로필 이미지 삭제 실패: {}", e.getMessage());
            }
        }

        user.updateProfileImages(result.turtleImageUrl(), result.backgroundImageUrl());
        userRepository.save(user);

        return result;
    }
}