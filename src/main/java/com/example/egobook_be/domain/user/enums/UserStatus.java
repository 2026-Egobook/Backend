package com.example.egobook_be.domain.user.enums;

public enum UserStatus {
    ACTIVE,         // 활동 중
    DORMANT,        // 휴면
    WITHDRAW_PENDING,// 탈퇴 대기 (완전 삭제 전 유예 기간)
    WITHDRAW,         // 탈퇴됨
    SUSPENDED,  // 정지됨
}
