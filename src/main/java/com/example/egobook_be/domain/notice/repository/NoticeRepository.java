package com.example.egobook_be.domain.notice.repository;

import com.example.egobook_be.domain.notice.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 관리자용 공지 목록을 발행 시각 최신순으로 조회한다.
     * @param pageable : 페이징 정보
     * @return : 공지 Slice
     */
    Slice<Notice> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /**
     * 현재 노출 중인(발행 시각이 지난) 공지 중 가장 최근 것을 조회한다.
     * - publishedAt이 같은 공지가 있을 때 순서가 흔들리지 않도록 id를 보조 정렬 기준으로 둔다.
     * @param now : 기준 시각
     * @return : 최신 공지 (없으면 Optional.empty())
     */
    Optional<Notice> findFirstByPublishedAtLessThanEqualOrderByPublishedAtDescIdDesc(LocalDateTime now);
}
