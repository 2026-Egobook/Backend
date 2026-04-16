package com.example.egobook_be.global.config;

import com.example.egobook_be.global.security.JwtAuthFilter;
import com.example.egobook_be.global.security.handler.JwtAccessDeniedHandler;
import com.example.egobook_be.global.security.handler.UserAuthenticationEntryPoint;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity

// @EnableMethodSecurity: 메서드 수준에서의 보안 처리를 활성화 해주는 어노테이션 (@Secure, @PreAuthorize 어노테이션 사용 가능함)
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    // CorsConfig에서 주입시킨 CorsConfigurationSource를 주입받는다
    private final CorsConfigurationSource corsConfigurationSource;
    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthFilter jwtAuthFilter;


    /*
     * 인증 없이 접근 가능한 화이트 리스트 URL 모음 String 배열 (로그인, 회원가입, 스웨거 등)
     */
    private static final String[] AUTH_WHITELIST = {
            "/auth/**", // /auth로 오는 요청은 전부 허용
            "/terms", // 약관 조회 요청은 허용
            "/v3/api-docs/**",  // Swagger JSON 데이터
            "/swagger-ui/**",   // Swagger UI CSS, JS, 이미지
            "/swagger-ui-custom.html",  // Swagger UI 메인 페이지
            "/manage/health",  // AWS의 ALB 헬스 체크 경로
            "/manage/prometheus", // Prometheus Metrics 수집 경로
            "/ads/admob/callback", // AdMob의 광고 보상 수령 여부 확인 경로
            "/admin/auth/refresh",
            "/admin/auth/recertification",
    };

    /**
     * Spring Security의 FilterChain 설정 - 각 Request마다 해당 filterChain에 등록된 필터들이 순서대로 실행된다.
     * @param http : HttpSecurity는 SecurityFilterChain을 조립하는 Builder(DSL)이다.
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*
         * 1. CSRF 방어 기능 비활성화
         * JWT 기반 Stateless API에서는 폼 로그인-세션 기반 인증이 아니므로 CSRF 방어 기능이 필요 없음
         */
        http.csrf(AbstractHttpConfigurer::disable);

        /*
         * 2. Cors 설정
         * http의 .cors() 안에 configurationSource를 명시적으로 지정한다.
         */
        http.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource));

        /*
         * 3. 세션 관리
         * 세션을 사용하지 않도록 설정한다.
         */
        http.sessionManagement(sessionManageMent -> sessionManageMent.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
        ));

        /*
         * 4. 폼 로그인 & HTTP Basic 끄기
         * 폼 로그인(세션)과 Basic 인증을 꺼서 JWT만 사용하도록 설정함
         */
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        /*
         * 5. JWT 필터 등록
         * UsernamePasswordAuthenticationFilter 앞에 직접 만든 JwtAuthFilter를 넣는다.
         * [ http.addFilterBefore("추가할 필터", 기존 필터) ]
         * 해당 필터로 JWT를 통한 인증 처리를 수행하게 한다.
         */
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        /*
         * 6. 예외 처리 핸들러 설정 - 인증 실패(401) 및 접근 거부(403) 예외를 처리하는 핸들러 설정
         */
        http.exceptionHandling(e -> e
                .authenticationEntryPoint(userAuthenticationEntryPoint) // 401 에러 핸들러
                .accessDeniedHandler(jwtAccessDeniedHandler)            // 403 에러 핸들러
        );

        /*
         * 6. 권한 규칙 작성
         * - 화이트 리스트에 있는 경로는 누구나 접근할 수 있도록 허용한다.
         * - "/admin/**"로 요청이 왔을 때, 해당 사용자가 "ADMIN" Role을 갖고 있을 때만 접속을 허용한다.
         */
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(AUTH_WHITELIST).permitAll()
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
