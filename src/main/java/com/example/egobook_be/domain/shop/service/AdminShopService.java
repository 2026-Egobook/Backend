package com.example.egobook_be.domain.shop.service;

import com.example.egobook_be.domain.shop.dto.AdminItemListResDto;
import com.example.egobook_be.domain.shop.dto.AdminItemReqDto;
import com.example.egobook_be.domain.shop.dto.AdminItemResDto;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.infra.s3.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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
    public AdminItemResDto createItem(AdminItemReqDto reqDto) throws IOException {
        String path=reqDto.category().name().toLowerCase();

        // 기존 path 저장방법과 일치시킴
        path = path.equals("decor_two") ? "decor2" : path;
        path = path.equals("decor_one") ? "decor1" : path;

        // 이미지 s3에 업로드
        String imageUrl = s3ImageService.upload(reqDto.file(),path);

        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);


        String itemStatus = (reqDto.status() != null && reqDto.status().equalsIgnoreCase("inactive")) ? "INACTIVE" : "ACTIVE";

        Item item = Item.builder()
                .category(reqDto.category())
                .price(reqDto.price())
                .path(path)
                .name(fileName)
                .status(itemStatus)
                .build();

        Item savedItem= itemRepository.save(item);
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
                .map(item -> new AdminItemResDto(
                        item.getId(),
                        item.getPath(),
                        item.getCategory(),
                        item.getName(),
                        item.getPrice(),
                        item.getFullUrl(cloudfrontDomain + "/shop"),
                        item.getStatus(),
                        item.getCreatedAt()
                )).toList();

        return new AdminItemListResDto(items, slice.hasNext());
    }

    // 수정 (파일 포함 요청이므로 POST 사용)
    @Transactional
    public AdminItemResDto updateItem(Long itemId, AdminItemReqDto reqDto) throws IOException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));

        String newPath = reqDto.category().name().toLowerCase();
        newPath = newPath.equals("decor_two") ? "decor2" : newPath;
        newPath = newPath.equals("decor_one") ? "decor1" : newPath;
        String finalFileName = item.getName();

        // 파일 업로드 없이 카테고리만 변경된 경우 이미지의 저장 경로(카테고리) 경로 수정
        if ((reqDto.file() == null || reqDto.file().isEmpty()) && !newPath.equals(item.getPath())) {
            s3ImageService.move(item.getPath(), newPath, item.getName());
        }

        // 파일이 변경된 경우 이미지 삭제 후 재업로드
        if (reqDto.file() != null && !reqDto.file().isEmpty()) {
            s3ImageService.delete(item.getPath(), item.getName());
            s3ImageService.upload(reqDto.file(), newPath);
            finalFileName = reqDto.file().getOriginalFilename();
        }

        item.updateAll(
                reqDto.category(),
                reqDto.price(),
                newPath,
                finalFileName,
                reqDto.status()
        );

        return AdminItemResDto.builder()
                .id(item.getId())
                .path(item.getPath())
                .category(item.getCategory())
                .name(item.getName())
                .price(item.getPrice())
                .status(item.getStatus())
                .imageUrl(cloudfrontDomain + "/shop/" + item.getPath() + "/" + item.getName())
                .createdAt(item.getCreatedAt())
                .build();
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
        return new AdminItemResDto(
                item.getId(),
                item.getPath(),
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getFullUrl(cloudfrontDomain + "/shop"),
                item.getStatus(),
                item.getCreatedAt()
        );
    }
}