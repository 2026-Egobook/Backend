package com.example.egobook_be.domain.auth.sevice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VerifyTokenTest {

    @Test
    @DisplayName("[성공] Google ID Token 검증 후 Token의 Payload 반환")
    void successVerifyTokenAndReturnPayload(){


    }

    @Test
    @DisplayName("[실패 1] GoogleIdTokenVerifier verify 결과값이 null인 경우")
    void failGoogleIdTokenVerifierMethodVerifyResultNull(){

    }

    @Test
    @DisplayName("[실패 2] Token 검증 시 GeneralSecurityException, IOException 발생")
    void failGeneralSecurityExceptionIOException(){

    }

    @Test
    @DisplayName("[실패 3] Token 검증 시 IllegalArgumentException 발생")
    void failIllegalArgumentException(){

    }


}
