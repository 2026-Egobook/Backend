package com.example.egobook_be.global.util;

import com.google.crypto.tink.subtle.Base64;
import com.google.crypto.tink.subtle.EcdsaVerifyJce;
import com.google.crypto.tink.subtle.EllipticCurves;
import com.google.crypto.tink.subtle.EllipticCurves.EcdsaEncoding;
import com.google.crypto.tink.subtle.Enums.HashType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AdMobVerifier {
    // 구글의 공개키 목록을 제공하는 URL (고정값)
    private static final String ADMOB_KEYS_URL = "https://www.gstatic.com/admob/reward/verifier-keys.json";

    // 키를 메모리에 캐싱하는 저장소 (Thread-Safe)
    private final Map<Long, ECPublicKey> publicKeys = new ConcurrentHashMap<>();

    /**
     * 서버가 시작될 때 자동으로 실행되어 키를 가져옵니다.
     * (사용자가 처음 요청할 때 기다리지 않게 하기 위함)
     */
    @PostConstruct
    public void init() {
        try {
            fetchPublicKeys();
            log.info("[AdMobVerifier] 구글의 Public Key들이 성공적으로 로드되었습니다. Count: {}", publicKeys.size());
        } catch (Exception e) {
            log.error("[AdMobVerifier] 서버 시작 시 구글의 Public Key들을 불러오는데 실패했습니다. Will retry on request.", e);
        }
    }

    /**
     * AdMob가 보낸 서명을 통해, 해당 요청이 구글에서 온 것인지 검증하는 메서드
     * @param queryString : URL의 전체 쿼리 스트링 (user_id=...&signature=...)
     * @param signature : 서명 값
     * @param keyId : 키 ID
     * @return 검증 성공 여부
     */
    public boolean verify(String queryString, String signature, Long keyId) {
        try {
            // 1. 키가 없으면 다시 가져오기 시도 (Lazy Loading)
            if (!publicKeys.containsKey(keyId)) {
                log.warn("[AdMobVerifier] Public Key ID {}를 캐시에서 찾지 못했습니다. 다시 Public Key들을 가져오는 중입니다...", keyId);
                fetchPublicKeys();
            }

            // 2. 그래도 없으면 실패
            ECPublicKey publicKey = publicKeys.get(keyId);
            if (publicKey == null) {
                log.error("[AdMobVerifier] 재시도 후에도 해당 Key ID {}를 캐시에서 찾지 못했습니다.", keyId);
                return false;
            }

            /* 3. 검증할 데이터 추출
             * - AdMob 규격: "signature" 파라미터 직전까지의 모든 문자열이 원본 데이터임
             * - 예: "user=A&reward=10&signature=..." -> "user=A&reward=10" 부분이 데이터
             */
            int signatureIndex = queryString.indexOf("&signature");
            if (signatureIndex == -1) {
                // 맨 앞에 signature가 올 경우 (드물지만 방어 코드)
                signatureIndex = queryString.indexOf("signature");
            }

            String content = queryString.substring(0, signatureIndex);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.urlSafeDecode(signature);

            /*
             * 4. ECDSA(Elliptic Curve Digital Signature Algorithm): '타원 곡선 디지털 서명 알고리즘' 암호화 검증 수행 (Tink 라이브러리에 들어있음)
             * - contentByte를 SHA-256 알고리즘으로 돌린 해시값 생성
             * - signatureBytes를 publicKey, 타원곡선 알고리즘(ECDSA)을 이용해서 복호화한 해시값
             * => 이 두 값이 같은지 검증하는 과정이다.
             */
            EcdsaVerifyJce verifier = new EcdsaVerifyJce(publicKey, HashType.SHA256, EcdsaEncoding.DER);
            verifier.verify(signatureBytes, contentBytes);

            return true; // 예외가 안 나면 성공
        } catch (GeneralSecurityException e) {
            log.warn("[AdMobVerifier] Signature verification failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[AdMobVerifier] Internal Error during verification", e);
            return false;
        }
    }

    /**
     * 구글 서버에서 공개키 JSON을 받아와 파싱하는 로직
     */
    private void fetchPublicKeys() throws Exception {
        URL url = new URL(ADMOB_KEYS_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 5초 타임아웃
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                content.append(inputLine);
            }

            // JSON 파싱
            JSONObject jsonObject = new JSONObject(content.toString());
            JSONArray keys = jsonObject.getJSONArray("keys");

            for (int i = 0; i < keys.length(); i++) {
                JSONObject key = keys.getJSONObject(i);
                long keyId = key.getLong("keyId");
                String publicKeyBase64 = key.getString("base64"); // PEM 형식

                // Base64 -> PublicKey 객체 변환
                ECPublicKey ecPublicKey = EllipticCurves.getEcPublicKey(Base64.decode(publicKeyBase64));
                publicKeys.put(keyId, ecPublicKey);
            }
        }
    }
}