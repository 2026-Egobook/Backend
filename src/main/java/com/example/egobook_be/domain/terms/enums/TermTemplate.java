package com.example.egobook_be.domain.terms.enums;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermTemplate {
    TERM_OF_SERVICE(
            TermType.TERM_OF_SERVICE,
            "서비스 이용 약관",
            "https://bevel-beetle-a49.notion.site/2f638a539ac5801aa872e99ec4282f28?source=copy_link",
            true
    ),
    TERM_OF_PRIVACY_POLICY(
            TermType.TERM_OF_PRIVACY_POLICY,
            "개인정보 수집 및 이용",
            "https://bevel-beetle-a49.notion.site/2f638a539ac58059b9a1c883ad7d7164?source=copy_link",
            true
    );

    private final TermType termType;
    private final String description;
    private final String context;
    private final boolean required;
}
