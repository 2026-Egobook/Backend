package com.example.egobook_be.global.util;

import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * String 값을 Hashing(암호화)하는 유틸 클래스
 */
@Component
public class HashingUtil {
    /**
     * HmacSHA256 암호화 알고리즘을 위해 사용할 Salt값 application-???.yml에서 가져오기
      */
    @Value("${app.auth.device-salt}")
    private String salt;
    /**
     * HmacSHA256은 결정론적(같은 입력, 같은 salt값이라면 매번 같은 해시값이 나옴)이므로, 해당 알고리즘을 사용한다.
     * 복호화가 불가능하다.
     */
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * javax.crypto를 이용한 HMAC(Hash-based Message Authentication Code) 알고리즘으로 함수의 인자로 들어온 값을 해싱(보안화) 해주는 유틸 함수
     * @param value : 해싱을 수행할 value
     * @return 해싱된 value
     */
    public String hashingValue(String value) {
        try {
            /**
             * 1. 자바의 보안 팩토리에서 Mac(메시지 인증 코드) 생성 기계 빌려오기
             * - Mac Class: 데이터의 무결성을 검증하고 인증하는 데 사용되는 암호화 엔진
             * - Mac.getInstance(ALGORITHM): HmacSHA256 알고리즘을 처리할 수 있는 기계를 요청하는 것
             */
            Mac mac = Mac.getInstance(ALGORITHM);

            /**
             * 2. salt 값을 암호화 알고리즘이 이해할 수 있는 Key 객체(SecretKeySpec)로 변환한다.
             * salt의 byte값(utf-8)을 받아 SecretKeySpec 인스턴스를 생성한다.
             * - SecretKeySpec: 단순한 byte 배열(bute[])을 자바가 이해할 수 있는 Key 객체로 변환해주는 Wrapper
             */
            SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            /**
             * 3. 준비된 암호화 엔진(Mac)에 비밀키를 초기화한다.
             */
            mac.init(secretKeySpec);

            /**
             * 4. 암호화 엔진에 value 값을 넣어 암호화된 Byte 배열로 변환한다.
             * - mac.doFinal(...): 입력받은 byte 배열과 비밀키(salt)를 SHA-256 알고리즘으로 섞는다.
             */
            byte[] hashBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

            /**
             * 5. byte 배열(이진 데이터)을 Base64(ASCII)로 인코딩한다.
             */
            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception e) {
            throw new CustomException(GlobalErrorCode.HASHING_FAILED);
        }
    }

}
