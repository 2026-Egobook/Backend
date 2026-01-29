package com.example.egobook_be.domain.psychology.dto;

public record KnowledgeDeleteResDto( boolean deleted, Long knowledgeId, String toastMessage ) {}