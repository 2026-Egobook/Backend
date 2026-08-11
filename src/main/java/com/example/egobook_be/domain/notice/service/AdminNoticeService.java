package com.example.egobook_be.domain.notice.service;

import com.example.egobook_be.domain.notice.dto.NoticeAdminResDto;
import com.example.egobook_be.domain.notice.dto.NoticeCreateReqDto;
import com.example.egobook_be.domain.notice.dto.NoticeReadResetResDto;
import com.example.egobook_be.domain.notice.dto.NoticeUpdateReqDto;
import com.example.egobook_be.domain.notice.entity.Notice;
import com.example.egobook_be.domain.notice.exception.NoticeErrorCode;
import com.example.egobook_be.domain.notice.mapper.NoticeMapper;
import com.example.egobook_be.domain.notice.repository.NoticeReadRepository;
import com.example.egobook_be.domain.notice.repository.NoticeRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final NoticeMapper noticeMapper;

    /**
     * 공지사항을 등록한다.
     * @param reqDto : 제목, 노션 링크, 발행 일시
     * @return : 등록된 공지 정보
     */
    @Transactional
    public NoticeAdminResDto createNotice(NoticeCreateReqDto reqDto) {
        log.info("[AdminNoticeService] createNotice() - START | title: {}", reqDto.title());

        Notice notice = Notice.builder()
                .title(reqDto.title())
                .notionUrl(reqDto.notionUrl())
                .publishedAt(reqDto.publishedAt())
                .build();
        Notice saved = noticeRepository.save(notice);
        NoticeAdminResDto result = noticeMapper.toNoticeAdminResDto(saved);

        log.info("[AdminNoticeService] createNotice() - END | noticeId: {}", saved.getId());
        return result;
    }

    /**
     * 공지사항 목록을 발행 시각 최신순으로 조회한다.
     * @param page : 페이지 번호 (1 ~ N, 프론트 기준)
     * @param size : 페이지 크기
     * @return : SliceResponse<NoticeAdminResDto>
     */
    @Transactional(readOnly = true)
    public SliceResponse<NoticeAdminResDto> getNotices(int page, int size) {
        log.info("[AdminNoticeService] getNotices() - START | page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        SliceResponse<NoticeAdminResDto> result = SliceResponse.of(
                noticeRepository.findAllByOrderByPublishedAtDesc(pageable),
                noticeMapper::toNoticeAdminResDto
        );

        log.info("[AdminNoticeService] getNotices() - END | resultSize: {}", result.size());
        return result;
    }

    /**
     * 공지사항을 수정한다.
     * - 삭제 후 재등록 시 유저의 읽음 상태가 초기화되어 레드닷이 재노출되는 부작용을 막기 위해 별도 제공
     * @param noticeId : 수정할 공지 PK
     * @param reqDto : 수정할 제목, 노션 링크, 발행 일시
     * @return : 수정된 공지 정보
     */
    @Transactional
    public NoticeAdminResDto updateNotice(Long noticeId, NoticeUpdateReqDto reqDto) {
        log.info("[AdminNoticeService] updateNotice() - START | noticeId: {}", noticeId);

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(NoticeErrorCode.NOTICE_NOT_FOUND));
        notice.update(reqDto.title(), reqDto.notionUrl(), reqDto.publishedAt());
        NoticeAdminResDto result = noticeMapper.toNoticeAdminResDto(notice);

        log.info("[AdminNoticeService] updateNotice() - END | noticeId: {}", noticeId);
        return result;
    }

    /**
     * 공지사항을 삭제한다.
     * @param noticeId : 삭제할 공지 PK
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        log.info("[AdminNoticeService] deleteNotice() - START | noticeId: {}", noticeId);

        if (!noticeRepository.existsById(noticeId)) {
            throw new CustomException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        noticeRepository.deleteById(noticeId);

        log.info("[AdminNoticeService] deleteNotice() - END | noticeId: {}", noticeId);
    }

    /**
     * 현재 노출 중인 최신 공지의 읽음 기록을 전부 지워 모든 유저에게 레드닷을 다시 노출시킨다.
     * - 읽음 row가 없으면 안 읽음으로 간주하는 구조라, 삭제만으로 전체 레드닷이 켜진다.
     * @return : 대상 공지 정보와 해제된 읽음 기록 수
     */
    @Transactional
    public NoticeReadResetResDto resetNoticeReads() {
        log.info("[AdminNoticeService] resetNoticeReads() - START");

        LocalDateTime now = LocalDateTime.now();
        Notice notice = noticeRepository
                .findFirstByPublishedAtLessThanEqualOrderByPublishedAtDesc(now)
                .orElseThrow(() -> new CustomException(NoticeErrorCode.NO_PUBLISHED_NOTICE));

        int clearedReadCount = noticeReadRepository.deleteAllByNoticeId(notice.getId());

        log.info("[AdminNoticeService] resetNoticeReads() - END | noticeId: {}, clearedReadCount: {}",
                notice.getId(), clearedReadCount);
        return new NoticeReadResetResDto(notice.getId(), notice.getTitle(), clearedReadCount, now);
    }
}
