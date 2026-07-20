package com.example.egobook_be.domain.notice.mapper;

import com.example.egobook_be.domain.notice.dto.NoticeAdminResDto;
import com.example.egobook_be.domain.notice.entity.Notice;
import org.springframework.stereotype.Component;

@Component
public class NoticeMapper {

    /**
     * Notice Entity -> NoticeAdminResDto 변환
     * @param notice : 변환할 Notice Entity
     * @return : 변환된 NoticeAdminResDto
     */
    public NoticeAdminResDto toNoticeAdminResDto(Notice notice) {
        return NoticeAdminResDto.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .notionUrl(notice.getNotionUrl())
                .publishedAt(notice.getPublishedAt())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
