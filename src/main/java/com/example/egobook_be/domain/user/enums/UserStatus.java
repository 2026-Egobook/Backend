package com.example.egobook_be.domain.user.enums;

public enum UserStatus {
    ACTIVE,         // 활동 중
    DORMANT,        // 휴면
    WITHDRAW_PENDING,// 탈퇴 대기 (완전 삭제 전 유예 기간)
    WITHDRAW,         // 탈퇴됨
    SUSPENDED,  // 정지됨 (편지, 오늘의 질문 답변에 대한 제재가 아니라, 문제가 생겼을 경우 계정 자체를 완전 정지시키는 경우)
}
