package com.example.egobook_be.domain.psychology.dto;

import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import lombok.Getter;

@Getter
public class PsychologyKnowledgeResponse {
    private Long id;
    private String content;
    private String source;

    public PsychologyKnowledgeResponse(PsychologyKnowledge entity) {
        this.id = entity.getId();
        this.content = entity.getContent();
        this.source = entity.getSource();
    }
}