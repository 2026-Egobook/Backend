package com.example.egobook_be.domain.terms.entity;

import com.example.egobook_be.domain.terms.enums.TermTemplate;
import com.example.egobook_be.domain.terms.enums.TermType;
import com.example.egobook_be.domain.terms.enums.TermVersion;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "term")
public class Term extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TermType termType;

    @Column(name = "description", nullable = false)
    private String description; // 해당 약관 한국어 텍스트

    @Column(name = "term_version", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TermVersion termVersion = TermVersion.V1;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "required", nullable = false)
    private boolean required;


    public Term(TermTemplate termTemplate, TermVersion termVersion) {
        this.termType = termTemplate.getTermType();
        this.description = termTemplate.getDescription();
        this.termVersion = termVersion;
        this.context = termTemplate.getContext();
        this.required = termTemplate.isRequired();
    }

    /**
     * 해당 약관의 내용을 전부 수정하는 함수
     */
    public void updateTerm(TermTemplate termTemplate, TermVersion termVersion) {
        this.termType = termTemplate.getTermType();
        this.description = termTemplate.getDescription();
        this.termVersion = termVersion;
        this.context = termTemplate.getContext();
        this.required = termTemplate.isRequired();
    }

    /**
     * 약관의 내용 & 버전을 변경하는 함수
     */
    public void updateContext(String context, TermVersion termVersion) {
        this.context = context;
        this.termVersion = termVersion;
    }
}
