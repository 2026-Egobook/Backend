package com.example.egobook_be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Spring Context에 CORS 정책을 Bean으로 등록하는 함수
     * @return CorsConfigurationSource : 현재 요청에 맞는 CORS 규칙(Configuration)을 찾아주는 역할을 하는 인터페이스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CorsConfiguration: Cors 규칙을 담고 있는 Class
        CorsConfiguration config = new CorsConfiguration();

        /*
         * 서버가 응답 헤더로 아래 헤더를 프론트에게 내려주게 만드는 설정
         *  [ Access-Control-Allow-Credentials: true ]
         *  Access-Control-Allow-Credentials: CORS에서 브라우저가 "인증 정보(credentials)를 포함한 요청 & 응답"을 허용해도 되는지를 서버가 선언하는 응답 헤더
         *  프론트에서도 "credential 포함" 요청을 보내야 Credential Request를 사용할 수 있다.
         */
        config.setAllowCredentials(true);

        /*
         * 서버가 응답 헤더로 아래 헤더를 프론트에게 내려주게 만드는 설정
         *  [ Access-Control-Allow-Origin: <요청한_Origin_주소들> ]
         *  Access-Control-Allow-Origin: 허용할 클라이언트의 주소 Origin을 담는 헤더. 프론트는 해당 값을 보고 본인의 Origin과 비교하여, 값이 같지 않은 경우에는 사용자에게 응답을 주지 않는다.
         *  브라우저의 SOP 설정 조건부 해제를 위한 설정
         *  setAllowedOrigins 대신 setAllowedOriginPatterns를 사용해야 와일드카드(*)와 Credentials를 동시에 쓸 수 있음
         */
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173", // 안드로이드는 cors 설정이 필요 없으나, 관리자용 웹 페이지를 위한 cors 설정을 남겨둠
                "https://*.egobook.site" // 프론트 배포 도메인에 따라서 달라짐
        ));

        /*
         * 서버가 응답 헤더로 아래 헤더를 프론트에게 내려주게 만드는 설정
         *  [ Access-Control-Allow-Methods: <허용할 http 메서드들> ]
         *  Access-Control-Allow-Methods: 브라우저가 보내는 Preflight Request(사전요청, OPTIONS 메서드)에 대한 응답으로, 서버가 허용하는 HTTP Method의 리스트를 명시하는 헤더
         */
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        /*
         * 서버가 응답 헤더로 아래 헤더를 프론트에게 내려주게 만드는 설정
         *  [ Access-Control-Allow-Headers: <허용할 헤더들> ]
         *  Access-Control-Allow-Headers: 프론트엔드가 요청을 보낼 때, 어떤 커스텀 헤더를 달아서 보내도 되는지 설정해주는 헤더
         *  - 브라우저가 Preflight(OPTIONS) 요청 시 Access-Control-Request-Headers: Authorization, X-My-Header라고 물어본다.
         *  - 서버가 Access-Control-Allow-Headers: * 라고 응답하면, 브라우저는 모든 헤더 전송을 허용한다. (바람직하지 않음)
         */
        config.setAllowedHeaders(List.of(
                "Authorization",      // JWT 등 인증 토큰 전송용
                "Content-Type",       // application/json 등 데이터 타입 명시
                "X-Requested-With",   // AJAX 요청 식별
                "Accept",             // 클라이언트가 처리가능한 미디어 타입
                "Origin",             // CORS 검증
                "Access-Control-Request-Method", // 브라우저가 Preflight 단계에서 사용되는 헤더
                "Access-Control-Request-Headers"
        ));

        /*
         * 서버가 응답 헤더로 아래 헤더를 프론트에게 내려주게 만드는 설정
         *  [ Access-Control-Expose-Headers: <허용할 헤더들> ]
         *  Access-Control-Expose-Headers: 서버가 클라이언트에게 보내는 응답에 포함된 헤더들 중, 클라이언트가 접근할 수 없는 헤더들에 대해 접근할 수 있도록 설정하는 헤더
         *  - 서버가 Access-Control-Expose-Headers: * 라고 응답하면, 브라우저는 모든 헤더값을 읽을 수 있다. (바람직하지 않음)
         */
        config.setExposedHeaders(List.of(
                "Authorization",      // 토큰 재발급 시 헤더로 새 토큰을 내려준다면 필수
                "X-Custom-Header",    // 커스텀하게 내려주는 헤더가 있다면 추가
                "Content-Disposition" // 파일 다운로드 시 파일명 등을 읽어야 한다면 필수
        ));
            // - Content-Type 등 기본 헤더는 설정 안 해도 읽을 수 있음

        // 1. 소스 생성: URL 별로 CORS 설정을 관리하는 관리자 객체 생성
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 2. 등록: "모든 경로(/**)"에 대해 미리 정의한 "규칙(config)"을 적용하겠다.
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
