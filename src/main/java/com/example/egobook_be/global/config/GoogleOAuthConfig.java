package com.example.egobook_be.global.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class GoogleOAuthConfig {
    @Value("${app.oauth.google.android-client-ids}")
    private String clientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(){
        /*
         * NetHttpTransport: 구글 서버와 통신하기 위한 HTTP 전송 계층
         * GsonFactory: JSON 데이터를 파싱하기 위한 팩토리 (JacksonFactory를 사용하기도 함)
         * .setAudience(...): 해당 토큰이 해당 서비스(Client ID)를 위해 발급된 것인지 확인하기 위해, 비교를 위한 토큰을 설정한다.
         */
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }
}
