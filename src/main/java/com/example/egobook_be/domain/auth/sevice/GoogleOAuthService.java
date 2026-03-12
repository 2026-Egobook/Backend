package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.global.exception.CustomException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 클라이언트로부터 전달받은 ID(Token)의 유효성을 검증하고, 사용자 정보를 추출하는 서비스 클래스
 * 클라이언트가 구글 로그인 성공 후 받아온 입장권(ID Token)이 위조되지 않았는지, 만료되지 않았는지 서버 측에서 최종 확인한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {
    private final GoogleIdTokenVerifier verifier; // 구글 ID

    /**
     * 클라이언트에게 받은 ID Token을 검증하고 Payload(유저 정보)를 반환하는 함수
     * @param idTokenString 프론트에서 받은 ID Token
     * @return GoogleIdToken.Payload (email, sub 등 포함)
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            /*
             * 1. GoogleIdTokenVerifier로 사용자에게 받은 String 형태의 idToken을 검증한다.
             * - Header, Payload, Signature 구조가 올바른지 확인한다.
             * - 구글의 Public Key로 서명이 유효한지 확인한다.(공개 키는 자동으로 가져와서 확인한다)
             * - 토큰의 exp 클레임을 확인하여, 서명이 유효한지 확인한다.
             * - account.google.com에서 발급된 토큰인지, 발급자(Issuer)를 확인한다.
             */
            GoogleIdToken idToken = verifier.verify(idTokenString);

            // 2. 검증 결과가 null인 경우 예외처리
            if (idToken == null) {
                log.warn("[Class:GoogleOAuthService]: Google Token이 null이거나 유효하지 않습니다.");
                throw new CustomException(AuthErrorCode.INVALID_GOOGLE_TOKEN);
            }
            // 3. 검증된 토큰의 Payload 반환
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error("[Class:GoogleOAuthService]: Token 검증 실패: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_GOOGLE_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("[Class:GoogleOAuthService]: Token 형식이 유효하지 않습니다: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }
}
