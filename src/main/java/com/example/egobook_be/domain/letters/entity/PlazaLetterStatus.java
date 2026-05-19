package com.example.egobook_be.domain.letters.entity;

public enum PlazaLetterStatus {
    WAITING,
    ARRIVED,
    DEFERRED,
    REPLIED,
    GAVE_UP,
    AI_REPLIED,
    SENT,
    ANALYZING,   //AI 분석 중
    CANCELLED,    //사용자가 취소함
    HIDDEN
}

