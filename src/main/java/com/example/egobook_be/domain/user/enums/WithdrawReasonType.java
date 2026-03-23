package com.example.egobook_be.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WithdrawReasonType {
    NOT_USED_OFTEN("자주 사용하지 않아요"),
    LACK_OF_CONTENT("콘텐츠나 기능이 부족해요"),
    INCONVENIENT_UI("앱 사용이 불편해요"),
    DIFFICULT_TO_COLLECT_INK("잉크를 모으기 어려워요"),
    OTHER("기타 (텍스트 필드)");

    // 각 Enum 상수가 가질 설명값 필드
    private final String description;
}
