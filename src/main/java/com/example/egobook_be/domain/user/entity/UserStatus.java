package com.example.egobook_be.domain.user.entity;

public enum UserStatus {
    NEW,            // 신규 가입
    ACTIVE,         // 활동 중
    DORMANT,        // 휴면
    DELETED_PENDING,// 삭제 대기 (완전 삭제 전 유예 기간)
    DELETED         // 삭제됨
}
