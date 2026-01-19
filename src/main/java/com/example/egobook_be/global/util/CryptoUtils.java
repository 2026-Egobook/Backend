package com.example.egobook_be.global.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 암호화 관련 유틸리티 클래스
 * 개발자가 HmacSHA256 해싱에 필요한 Salt 값을 랜덤으로 생성할 수 있도록 만든 유틸 클래스이다.
 */
public class CryptoUtils {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int SALT_LENGTH_BYTES = 32; // HmacSHA256의 보안 강도를 고려하여 Salt 길이를 32 bytes (256 bits)로 설정

    /**
     * 암호학적으로 안전한 무작위 Salt 값을 생성합니다.
     * @return Base64로 인코딩된 Salt 문자열
     */
    public static String generateRandomSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];

        // OS의 엔트로피를 이용하여 바이트 배열 채움
        secureRandom.nextBytes(salt);

        // 바이너리 데이터를 DB 저장 및 전송에 용이한 Base64 문자열로 변환
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 해당 함수를 실행시켜서, 콘솔에 나온 Salt값을 사용합니다.
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(CryptoUtils.generateRandomSalt());
    }
}