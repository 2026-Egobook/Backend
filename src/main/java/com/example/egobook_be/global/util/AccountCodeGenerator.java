package com.example.egobook_be.global.util;

import java.security.SecureRandom;

/**
 * User Entity의 accountCode를 랜덤으로 생성해주는 함수를 담은 유틸 클래스
 */
public class AccountCodeGenerator {
    // 1. 해당 Account Code를 생성하는데 사용될 문자들 지정
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // 2. Account Code 길이를 지정
    private static final int CODE_LENGTH = 12;
    // 3. SecureRandom: 시스템의 노이즈(엔트로피)를 이용하여 랜덤 숫자를 생성하는 클래스
    private static final SecureRandom random = new SecureRandom();

    /**
     * User Entity의 accountCode를 랜덤으로 생성해주는 함수
     * @return 랜덤으로 생성된 12줄 짜리 accountCode
     */
    public static String generateAccountCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for(int i = 0; i < CODE_LENGTH; i++){
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
