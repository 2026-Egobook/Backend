package com.example.egobook_be.domain.terms.loader;

import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.enums.TermTemplate;
import com.example.egobook_be.domain.terms.enums.TermVersion;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;

/**
 * 서버 시작 시, DB를 확인하여 약관 내용이 있으면 추가, 없으면 수정하는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(value = 1) // 실행 순서
public class TermInitializer implements ApplicationRunner {
    private final TermRepository termRepository;
    private final TermVersion termVersion = TermVersion.V1;

    @Override
    @Transactional
    public void run(ApplicationArguments args){
        log.info("🚀 사용자 약관 목록을 DB에 최신화합니다.");
        Arrays.stream(TermTemplate.values()).forEach(termTemplate -> {
            // 1. TermTemplate Enum 클래스에서, 각각의 항목들의 데이터가 Term table에 존재하는지 확인한다.
            Term existTerm = termRepository.findByTermType(termTemplate.getTermType()).orElse(null);

            // 2. 만약 해당 약관이 아직 Term Table에 존재하지 않다면, 해당 테이블에 추가한다.
            if(existTerm == null){
                Term newTerm = new Term(termTemplate, termVersion);
                termRepository.save(newTerm);
                log.info("✅ 약관 등록 완료: {}({})", termTemplate.getDescription(), termVersion);
            }
            // 3. 만약 해당 약관이 term Table에 존재한다면, 기존 term 내용을 새로운 값으로 수정한다.
            else{
                existTerm.updateTerm(termTemplate, termVersion);
                log.info("✅ 약관 수정 완료: {}({})", termTemplate.getDescription(), termVersion);
            }
        });
        log.info("🚀 약관 데이터 동기화 완료.");
    }
}
