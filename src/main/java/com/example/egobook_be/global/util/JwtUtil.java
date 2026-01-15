package com.example.egobook_be.global.util;

import com.example.egobook_be.global.enums.JwtTokenType;
import com.example.egobook_be.global.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 생성, 검증 등의 기능들을 하는 Util Class
 */
@Slf4j
@Component
public class JwtUtil {
    // SecretKey: 대칭키 암호에서 사용하는 Secret Key 표현
    private final SecretKey secretKey;
    // Duration: 시간의 길이 표현
    private final Duration accessExpiration;
    private final Duration refreshExpiration;
    private final Duration recoverExpiration;

    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret, // secretKey
            @Value("${jwt.access-token-validity-in-ms}") Long accessTokenExpTime, // accessToken 만료 시간(ms)
            @Value("${jwt.refresh-token-validity-in-ms}") Long refreshTokenExpTime, // refreshToken 만료 시간(ms)
            @Value("${jwt.recover-token-validity-in-ms}") Long recoverTokenExpTime // recoverToken 만료 시간(ms)
    ) {
        /**
         * HMAC-SHA 키 생성
         * - secret.getBytes(StandardCharsets.UTF_8): secret key byte 배열로 변환
         * - Keys.hmacShaKeyFor(...): 전달된 바이트 배열 기반으로 HMAC-SHA 계열 알고리즘용 SecretKey 생성
         */
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // Duration.ofMillis(): ms 단위의 숫자를 Duration 객체로 변환하는 메서드
        this.accessExpiration = Duration.ofMillis(accessTokenExpTime);
        this.refreshExpiration = Duration.ofMillis(refreshTokenExpTime);
        this.recoverExpiration = Duration.ofMillis(recoverTokenExpTime);
    }

    /**
     * Access Token 생성 함수
     * @param user CustomUserDetails
     * @return String 액세스 토큰
     */
    public String createAccessToken(CustomUserDetails user) {
        return createToken(user, accessExpiration, JwtTokenType.ACCESS.toString());
    }

    /**
     * Refresh Token 생성 함수
     * @param user CustomUserDetails
     * @return String Refresh 토큰
     */
    public String createRefreshToken(CustomUserDetails user) {
        return createToken(user, refreshExpiration, JwtTokenType.REFRESH.toString());
    }

    /**
     * Recover Token 생성 함수
     * @param user CustomUserDetails
     * @return String. Recover 토큰
     */
    public String createRecoverToken(CustomUserDetails user) {
        return createToken(user, recoverExpiration, JwtTokenType.RECOVER.toString());
    }

    /**
     * Token에 담겨있는 Claim들 중 subject 정보를 추출해서 가져오는 함수
     * @param token JWT Token
     * @return 해당 토큰에 담겨있는 subject ("provider:deviceUid")
     */
    public String getSubjectFromToken(String token) {
        return getPayload(token).getSubject(); // subject: 해당 토큰이 누구에 대한 것인지를 지정함
    }

    /**
     * Token에 담겨있는 Claim들 중 Jti(JWT Id)를 추출하는 함수
     * - 로그아웃(블랙리스트) 처리나 Refresh Token Rotation 검증 시 사용된다.
     */
    public String getJtiFromToken(String token) {
        return getPayload(token).getId();
    }

    /**
     * 해당 Token이 유효한지 확인하는 함수
     * - token에서 payload를 가져올 수 있으면 유효한 것이고, 아니면 유효하지 않은 것이다.
     * @param token JWT Token
     * @return 해당 Token이 유효한지 여부(true/false)
     */
    public boolean validateToken(String token) {
        try {
            getPayload(token); // 파싱 시도
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("유효하지 않은 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT Token을 생성하는 함수
     * @param user CustomUserDetails. 사용자의 정보가 담겨있음
     * @param expiration Duration. 유효기간
     * @param type 해당 토큰의 유형.
     * @return
     */
    private String createToken(CustomUserDetails user, Duration expiration, String type) {
        /**
         * 1. 현재 순간의 정보들을 Instant로 가져온다.
         * Instant: 타임라인 상의 특정 순간을 나타낸다.
         * - 즉, 절대적인 시간을 다룰 때 사용하는 표준이다.
         */
        Instant now = Instant.now();

        /**
         * 2. 권한 정보 가져오기
         * 함수 인자로 받은 CustomUserDetails를 이용하여 사용자의 권한들을 가져온다.
         * => "권한1,권한2" 형식으로
         */
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        /**
         * 3. UUID를 생성하여 Jti로 할당
         */
        String jti = UUID.randomUUID().toString();

        /**
         * 4. Jwt Token 생성
         */
        return Jwts.builder()
                .id(jti) // Jwt Id 설정
                .subject(user.getUsername()) // "provider:deviceUid" 형식의 융합 키
                .claim("role", authorities)  // Custom Claim: 권한
                .claim("type", type)         // Custom Claim: 토큰 타입
                .issuedAt(Date.from(now))    // 발행 시간
                .expiration(Date.from(now.plus(expiration))) // 만료 시간
                .signWith(secretKey)         // 서명
                .compact();
    }


    /**
     * 해당 토큰의 Type(ACCESS, REFRESH, RECOVER
     * @param token
     * @return
     */
    public JwtTokenType getTokenType(String token) {
        return JwtTokenType.valueOf(getPayload(token).get("type", String.class));
    }

    /**
     * JWT Token에서 Claims를 추출하는 함수
     * @param token JWT Token
     * @return
     */
    private Claims getPayload(String token) {
        return Jwts.parser() // JWT Parser를 만들기 위한 Builder 객체를 생성 (JWT를 해석, 검증할 도구를 조립하겠다고 선언하는 시작점)
                .verifyWith(secretKey) // 토큰의 Signature을 검증할 때 사용할 키를 설정
                .clockSkewSeconds(60) // 1분의 시간 오차 허용. 서버마다 시간이 다를 수 있으므로, JWT의 iat, exp같은 시간을 비교할 때 1분의 오차를 허용한다.
                .build()// JwtParser 객체 생성(JWT 문자열을 parsing하고, 클레임 검증 등을 수행할 수 있는 객체)
                .parseSignedClaims(token) // 실제 들어온 token 문자열을 파싱, 검증한다. (JWS<Claims> 객체 반환, JWS: 서명된 JWT)
                .getPayload(); // JWS<Claims>에서 Payload만 빼오는 함수
    }

}
