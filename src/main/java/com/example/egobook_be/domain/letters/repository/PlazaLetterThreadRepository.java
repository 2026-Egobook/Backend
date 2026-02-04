package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlazaLetterThreadRepository extends JpaRepository<PlazaLetterThread, Long> {
    /**
     * 연결된 편지(PlazaLetter)가 하나도 없는 빈 쓰레드를 일괄 삭제합니다.
     * (고아 쓰레드 정리)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PlazaLetterThread t WHERE NOT EXISTS (SELECT 1 FROM PlazaLetter l WHERE l.threadId = t.threadId)")
    void bulkDeleteEmptyThreads();
}

