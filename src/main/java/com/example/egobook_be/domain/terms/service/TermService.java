package com.example.egobook_be.domain.terms.service;

import com.example.egobook_be.domain.terms.dto.TermResDto;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermService {
    private final TermRepository termRepository;

    @Transactional(readOnly = true)
    public List<TermResDto> getTerms(){
        log.info("[TermService] getTerms Start");
        List<Term> terms = termRepository.findAll();
        List<TermResDto> termResDtoList = new ArrayList<>();
        terms.forEach(term ->
                termResDtoList.add(
                    TermResDto.builder()
                        .id(term.getId())
                        .termType(term.getTermType())
                        .termVersion(term.getTermVersion())
                        .description(term.getDescription())
                        .context(term.getContext())
                        .required(term.isRequired())
                        .build()
                )
        );
        log.info("[TermService] getTerms End");
        return termResDtoList;
    }
}
