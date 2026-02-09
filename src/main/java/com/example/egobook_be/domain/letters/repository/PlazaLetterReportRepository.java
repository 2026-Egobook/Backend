package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlazaLetterReportRepository extends JpaRepository<PlazaLetterReport, Long> {

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
}

