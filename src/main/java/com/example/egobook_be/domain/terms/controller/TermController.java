package com.example.egobook_be.domain.terms.controller;

import com.example.egobook_be.domain.terms.dto.TermResDto;
import com.example.egobook_be.domain.terms.service.TermService;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TermController implements TermControllerDocs{
    private final TermService termService;

    /**
     * [약관들 일괄 조회]
     * GET /terms
     */
    @Override
    public ResponseEntity<GlobalResponse<List<TermResDto>>> getTerms(){
        List<TermResDto> terms = termService.getTerms();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success(terms));
    }
}
