package com.example.egobook_be.domain.notice.repository;

import com.example.egobook_be.domain.notice.entity.Notice;
import com.example.egobook_be.domain.notice.entity.NoticeRead;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    /**
     * 유저가 해당 공지를 이미 읽었는지 확인한다.
     * @param user : 조회할 유저
     * @param notice : 조회할 공지
     * @return : 읽음 여부
     */
    boolean existsByUserAndNotice(User user, Notice notice);
}
