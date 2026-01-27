package com.example.egobook_be.domain.terms.service;

import com.example.egobook_be.domain.terms.dto.TermResDto;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {
    private final TermRepository termRepository;

    @Transactional(readOnly = true)
    public List<TermResDto> getTerms(){
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
        return termResDtoList;
    }
}
