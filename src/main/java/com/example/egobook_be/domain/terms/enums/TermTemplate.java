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
            """
                    서비스 이용 약관 임시 내용
                    """,
            true
    ),
    TERM_OF_PRIVACY_POLICY(
            TermType.TERM_OF_PRIVACY_POLICY,
            "개인정보 처리 방침",
            """
                    개인정보 처리 방침 임시 내용
                    """,
            true
    ),
    TERM_OF_PERSONAL_INFO_CONSENT(
            TermType.TERM_OF_PERSONAL_INFO_CONSENT,
            "개인정보 이용 동의 약관",
            """
                    개인정보 이용 동의 약관 임시 내용
                    """,
            false
    );

    private final TermType termType;
    private final String description;
    private final String context;
    private final boolean required;
}
