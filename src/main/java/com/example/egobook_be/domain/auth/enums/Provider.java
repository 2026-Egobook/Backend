package com.example.egobook_be.domain.auth.enums;

/**
 * 인증을 시도하는 주체가 누구인지 선언해둔 Enum Class
 */
public enum Provider {
    GUEST, GOOGLE;

    /**
     * 문자열을 받아서 일치하는 Provider를 찾고, 없으면 null을 반환하는 메서드
     * @param providerStr 검사하고자 하는 문자
     * @return 검사를 통과했다면 Provider 객체, 통과하지 못했다면 null 반환
     */
    public static Provider resolve(String providerStr) {
        providerStr = providerStr.toUpperCase(); // 모두 대문자로 변환
        for (Provider provider : Provider.values()) {
            if (provider.name().equals(providerStr)) {
                return provider;
            }
        }
        return null;
    }
}
