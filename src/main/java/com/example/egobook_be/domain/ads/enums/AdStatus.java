package com.example.egobook_be.domain.ads.enums;

public enum AdStatus {
    PENDING,    // 대기 중
    ACTIVE,     // 송출 중 (정상)
    PAUSED,     // 일시 정지 (관리자 조작)
    COMPLETED,  // 예산 소진으로 인한 종료
    DELETED     // 삭제됨 (Soft Delete)
}
