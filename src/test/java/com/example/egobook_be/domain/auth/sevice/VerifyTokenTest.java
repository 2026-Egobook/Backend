package com.example.egobook_be.domain.auth.sevice;

import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.global.exception.CustomException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class VerifyTokenTest {
    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    @Mock
    private GoogleIdTokenVerifier verifier;

    @Test
    @DisplayName("[성공] Google ID Token 검증 후 Token의 Payload 반환")
    void successVerifyTokenAndReturnPayload() throws Exception {
        // ========== Given ==========
        GoogleIdToken.Payload expectedPayload = new GoogleIdToken.Payload();
        expectedPayload.setSubject("subject");

        GoogleIdToken mockToken = new GoogleIdToken(new JsonWebSignature.Header(), expectedPayload, new byte[1], new byte[1]);
        given(verifier.verify(anyString())).willReturn(mockToken); // 테스트 메서드 선언부에 throws Exception 붙여야함 (내부에서 예외 던지기 때문)

        // ========== When ==========
        GoogleIdToken.Payload result = googleOAuthService.verifyToken("string");

        // ========== Then ==========
        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo("subject");
    }

    @Test
    @DisplayName("[실패 1] GoogleIdTokenVerifier verify 결과값이 null인 경우")
    void failGoogleIdTokenVerifierMethodVerifyResultNull() throws Exception {
        // ========== Given ==========
        given(verifier.verify(anyString())).willReturn(null);

        // ========== When & Then ==========
        CustomException exception = assertThrows(CustomException.class, () -> {
            googleOAuthService.verifyToken("error-token-string");
        });
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_GOOGLE_TOKEN);
    }

    @Test
    @DisplayName("[실패 2] Token 검증 시 GeneralSecurityException 발생")
    void failGeneralSecurityException() throws Exception {
        // Given
        given(verifier.verify(anyString())).willThrow(new GeneralSecurityException());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            googleOAuthService.verifyToken("error-token-string");
        });
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_GOOGLE_TOKEN);
    }

    @Test
    @DisplayName("[실패 3] Token 검증 시 IllegalArgumentException 발생")
    void failIllegalArgumentException() throws Exception {
        // Given
        given(verifier.verify(anyString())).willThrow(new IllegalArgumentException());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            googleOAuthService.verifyToken("error-token-string");
        });
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_GOOGLE_TOKEN);
    }


}
