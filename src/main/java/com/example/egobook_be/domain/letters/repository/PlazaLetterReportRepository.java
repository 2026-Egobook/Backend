package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.global.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlazaLetterReportRepository extends JpaRepository<PlazaLetterReport, Long>, PlazaLetterReportRepositoryCustom {

    boolean existsByLetter_LetterIdAndReporterId(Long letterId, Long reporterId);

    @Query("""
        select r.letter.letterId
        from PlazaLetterReport r
        where r.reporterId = :reporterId
          and r.letter.letterId in :letterIds
    """)
    List<Long> findReportedLetterIds(@Param("reporterId") Long reporterId,
                                     @Param("letterIds") List<Long> letterIds);

    @Query("select count(r) from PlazaLetterReport r where r.letter.letterId = :letterId")
    long countByLetter_LetterId(@Param("letterId") Long letterId);

    @Query("""
        SELECT r
        FROM PlazaLetterReport r
        JOIN FETCH r.letter
        ORDER BY r.createdAt DESC
    """)
    Slice<PlazaLetterReport> findAllWithLetter(Pageable pageable);

    //상세 조회
    @Query("""
        SELECT r
        FROM PlazaLetterReport r
        JOIN FETCH r.letter
        WHERE r.reportId = :reportId
    """)
    Optional<PlazaLetterReport> findByIdWithLetter(@Param("reportId") Long reportId);

    //수동 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PlazaLetterReport r WHERE r.letter.letterId = :letterId")
    void deleteAllByLetterId(@Param("letterId") Long letterId);

    @Query("SELECT COUNT(r) FROM PlazaLetterReport r WHERE r.letter.letterId = :letterId AND r.status = :status")
    long countByLetterIdAndStatus(@Param("letterId") Long letterId, @Param("status") ReportStatus status);
}

