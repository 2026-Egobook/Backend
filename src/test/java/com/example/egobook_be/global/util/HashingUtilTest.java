package com.example.egobook_be.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HashingUtilTest {
    private HashingUtil hashingUtil;

    // 테스트용 임시 비밀키 (실제 환경변수 아님)
    private static final String TEST_SALT = "test-secret-salt-key-1234567890";

    @BeforeEach
    void setUp() {
        // 1. 객체 생성 (Spring 컨텍스트 없이 순수 자바 객체로 생성)
        hashingUtil = new HashingUtil();

        // 2. 리플렉션을 사용하여 private 필드인 'salt'에 값을 강제로 주입
        // (Spring의 @Value가 하는 일을 테스트에서 흉내내는 것)
        ReflectionTestUtils.setField(hashingUtil, "salt", TEST_SALT);
    }

    @Test
    @DisplayName("동일한 Device UID와 Salt를 사용하면 항상 같은 해시값을 반환해야 한다")
    void hashDeviceUid_Deterministic() {
        // given
        String originalUid = "550e8400-e29b-41d4-a716-446655440000";

        // when
        String hash1 = hashingUtil.hashingValue(originalUid);
        String hash2 = hashingUtil.hashingValue(originalUid);

        // then
        assertThat(hash1).isNotNull();
        assertThat(hash1).isEqualTo(hash2); // 두 번 실행해도 결과가 같아야 함 (로그인 필수 조건)

        System.out.println("Input : " + originalUid);
        System.out.println("Hash  : " + hash1);
    }

    @Test
    @DisplayName("서로 다른 Device UID는 다른 해시값을 반환해야 한다")
    void hashDeviceUid_CollisionCheck() {
        // given
        String uidA = "uuid-aaaa-aaaa";
        String uidB = "uuid-bbbb-bbbb";

        // when
        String hashA = hashingUtil.hashingValue(uidA);
        String hashB = hashingUtil.hashingValue(uidB);

        // then
        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    @DisplayName("해싱된 결과는 Base64 포맷이어야 하며 길이가 일정 수준 이상이어야 한다")
    void hashDeviceUid_FormatCheck() {
        // given
        String uid = "test-uid";

        // when
        String result = hashingUtil.hashingValue(uid);

        // then
        // SHA-256(32byte)를 Base64로 인코딩하면 약 44자가 나옵니다.
        assertThat(result.length()).isGreaterThan(40);

        // Base64 문자열 패턴 매칭 (영문, 숫자, +, /, = 만 포함)
        assertThat(result).matches("^[A-Za-z0-9+/=]+$");
    }
}
