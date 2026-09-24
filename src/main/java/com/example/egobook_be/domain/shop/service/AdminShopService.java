package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.AdminItemListResDto;
import com.example.egobook_be.domain.shop.dto.AdminItemReqDto;
import com.example.egobook_be.domain.shop.dto.AdminItemResDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.exception.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.infra.s3.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@ Service
@RequiredArgsConstructor
public class AdminShopService {

    private final ItemRepository itemRepository;
    private final S3ImageService s3ImageService;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;


    @Transactional
    public AdminItemResDto createItem(AdminItemReqDto reqDto)
            throws IOException {

        if (reqDto.file() == null || reqDto.file().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "아이템 이미지는 필수입니다."
            );
        }

        String itemStatus = resolveStatus(
                reqDto.status(),
                "INACTIVE"
        );

        String path = resolvePath(reqDto.category());

        // 이미지 S3 업로드
        String imageUrl = s3ImageService.upload(
                reqDto.file(),
                path
        );

        // 실제 저장된 파일명 추출
        String fileName = imageUrl.substring(
                imageUrl.lastIndexOf("/") + 1
        );

        Item item = Item.builder()
                .category(reqDto.category())
                .price(reqDto.price())
                .path(path)
                .name(fileName)
                .status(itemStatus)
                .build();

        Item savedItem = itemRepository.save(item);

        return convertToAdminResDto(savedItem);
    }

    @Transactional(readOnly = true)
    public AdminItemListResDto getItemList(ItemCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Slice<Item> slice = itemRepository.findAll(pageable);

        if (category == null) {
            slice = itemRepository.findAll(pageable);
        } else {
            slice = itemRepository.findByCategory(category, pageable);
        }

        List<AdminItemResDto> items = slice.getContent().stream()
                .map(this::convertToAdminResDto).toList();

        return new AdminItemListResDto(items, slice.hasNext());
    }

    // 수정 (파일 포함 요청이므로 POST 사용)
    @Transactional
    public AdminItemResDto updateItem(
            Long itemId,
            AdminItemReqDto reqDto
    ) throws IOException {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new CustomException(ShopErrorCode.ITEM_NOT_FOUND)
                );

        String newStatus = resolveStatus(
                reqDto.status(),
                item.getStatus()
        );

        String oldPath = item.getPath();
        String oldFileName = item.getName();

        String newPath = resolvePath(reqDto.category());
        String finalFileName = oldFileName;

        boolean hasNewFile =
                reqDto.file() != null && !reqDto.file().isEmpty();

        boolean pathChanged = !newPath.equals(oldPath);

        if (hasNewFile) {
            // 새 이미지 업로드 후 실제 저장된 파일명 사용
            String imageUrl = s3ImageService.upload(
                    reqDto.file(),
                    newPath
            );

            finalFileName = imageUrl.substring(
                    imageUrl.lastIndexOf("/") + 1
            );

        } else if (pathChanged) {
            // 파일은 그대로 두고 카테고리만 변경
            s3ImageService.move(
                    oldPath,
                    newPath,
                    oldFileName
            );
        }

        item.updateAll(
                reqDto.category(),
                reqDto.price(),
                newPath,
                finalFileName,
                newStatus
        );

        AdminItemResDto response =
                convertToAdminResDto(itemRepository.save(item));

        // 파일 교체 후 기존 파일 정리는 별도로 수행
        return response;
    }

    // 삭제
    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));

        String path = item.getPath();
        String fileName = item.getName();

        // s3에서 이미지 삭제
        try {
            s3ImageService.delete(path, fileName);
            itemRepository.delete(item);
        } catch (Exception e) {
            throw new CustomException(ShopErrorCode.FILE_NOT_FOUND_IN_S3);
        }

    }

    // 활성/비활성 상태 전환
    @Transactional
    public String changeStatus(Long itemId, String status) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));
        if ("ACTIVE".equalsIgnoreCase(status)) {
             item.activate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
             item.deactivate();
        } else {
            throw new CustomException(ShopErrorCode.INVALID_ITEM_STATUS);
        }
        return item.getStatus();
    }

    private AdminItemResDto convertToAdminResDto(Item item) {
        AdminItemResDto resDto;
        if(item.getCategory().equals(ItemCategory.LETTER_PAPER)){
            resDto = AdminItemResDto.builder()
                    .id(item.getId())
                    .path(item.getPath())
                    .category(item.getCategory())
                    .name(item.getName())
                    .price(item.getPrice())
                    .imageUrl(item.getFullUrl(cloudfrontDomain))
                    .status(item.getStatus())
                    .createdAt(item.getCreatedAt())
                    .build();
        } else {
            resDto = AdminItemResDto.builder()
                    .id(item.getId())
                    .path(item.getPath())
                    .category(item.getCategory())
                    .name(item.getName())
                    .price(item.getPrice())
                    .imageUrl(item.getFullUrl(cloudfrontDomain+"/shop"))
                    .status(item.getStatus())
                    .createdAt(item.getCreatedAt())
                    .build();
        }
        return resDto;
    }

    private String resolveStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "ACTIVE";
        }

        if ("INACTIVE".equalsIgnoreCase(status)) {
            return "INACTIVE";
        }

        throw new CustomException(
                ShopErrorCode.INVALID_ITEM_STATUS
        );
    }

    private String resolvePath(ItemCategory category) {
        return switch (category) {
            case DECOR_ONE -> "decor1";
            case DECOR_TWO -> "decor2";
            case LETTER_PAPER -> "letter";
            default -> category.name().toLowerCase();
        };
    }
}