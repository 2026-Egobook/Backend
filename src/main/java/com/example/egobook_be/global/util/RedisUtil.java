package com.example.egobook_be.global.util;

import com.example.egobook_be.global.util.module.RedisValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.security.SignatureException;
import java.time.Duration;

/**
 * Redis 관련 Util 로직을 수행하는 Util 함수
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate; // Redis의 Key, Value가 모두 문자열일 때 사용하는 템플릿
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    // ==========================================================
    // [ Refresh Token ]
    // ==========================================================
    /**
     * 1. Refresh Token 정보 저장 (Set)
     * - Key: "RT:{hashedRefreshToken}" (Prefix를 붙여 관리 용이성 확보)
     * - Value: Value로 들어갈 Json 형태의 데이터들
     * - TTL: 만료 시간까지 남은 시간만큼 설정
     */
    public void setHashedRefreshTokenValue(String hashedRefreshToken, RedisValue value, long durationInMillis) {
        String key = "RT:" + hashedRefreshToken;
        try {
            String jsonValue = objectMapper.writeValueAsString(value);  // json 값을 String으로 변환한다.
            ValueOperations<String, String> values = redisTemplate.opsForValue(); // Redis의 자료구조들 중, String(Key-Value) 타입을 다루기 위한 연산자를 가져온다.
            values.set(key, jsonValue, Duration.ofMillis(durationInMillis)); // Key, Value, 해당 데이터의 TTL을 설정한다.(TTL이 끝나면 redis는 알아서 해당 데이터를 삭제한다.)
        } catch (JsonProcessingException e) {
            log.error("Redis Value JSON 변환 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Save Error");
        }
    }

    /**
     * 2. Refresh Token 정보 조회 (Get)
     * 해당 hashedRefreshToken으로 Redis에서 정보를 찾지 못하면 null을 반환한다.
     */
    public RedisValue getHashedRefreshTokenValue(String hashedRefreshToken) {
        String key = "RT:" + hashedRefreshToken;
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String jsonValue = values.get(key); // key를 이용하여 해당 key의 String 형태의 value를 가져온다.

        if (jsonValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonValue, RedisValue.class); // String 형태의 json 데이터를 실제 자바 객체로 변환(파싱)한다.
        } catch (JsonProcessingException e) {
            log.error("Redis Value JSON 파싱 에러: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 3. Refresh Token 삭제 (Delete)
     * - 로그아웃, 재발급 시 사용
     */
    public void deleteHashedRefreshToken(String hashedRefreshToken) {
        String key = "RT:" + hashedRefreshToken;
        if (!redisTemplate.hasKey(key)) {return;} // 해당 RefreshToken이 Redis에 없으면 굳이 delete하지 않음
        redisTemplate.delete(key); //
        log.info("[Redis] HashedRefreshToken이 Redis에서 삭제되었습니다. {}", hashedRefreshToken);
    }

    /**
     * 4. 해당 RefreshToken 존재 여부 확인
     */
    public boolean hasHashedRefreshToken(String hashedRefreshToken) {
        return redisTemplate.hasKey("RT:" + hashedRefreshToken);
    }

    // ==========================================================
    // [ BlackList ]
    // ==========================================================

    /**
     * 1. 해당 토큰의 Jti를 Redis의 블랙리스트에 등록하는 함수
     * - Key: BL: + JTI
     * - Value: TokenType
     * - TTL: 해당 토큰의 만료까지 남은 시간
     */
    public void setTokenInBlacklist(String token) {
        try{
            // 1. Redis에 등록할 Key, Value, Ttl 선언
            String key = "BL:" + jwtUtil.getJtiFromToken(token);
            String value = jwtUtil.getTokenType(token).name();
            long ttl = jwtUtil.getExpirationInMs(token) - System.currentTimeMillis();

            // 2. 이미 만료된 토큰인지 확인
            if(ttl <= 0) {
                log.warn("[Redis] 이미 만료된 Token이 블랙리스트 요청됨 - jti: {}", key);
                return;
            }

            // 3. 해당 Key가 이미 Redis에 존재하는지 확인
            boolean hasKey = redisTemplate.hasKey(key);
            if(hasKey) { return; }
            /*
             * 4. 해당 Key:Value Redis에 등록
             * - opsForValue(): Redis String(단일 값, Key:Value) 구조의 자료구조를 반환하는 함수
             */
            redisTemplate.opsForValue()
                    .set(key, value, Duration.ofMillis(ttl));
            log.info("[Redis] Token Redis BlackList에 등록 완료: {}, TTL - {}ms", key, ttl);
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // [보안] 위조되거나 손상된 토큰은 JTI를 추출할 수 없으므로 등록 불가 -> 로그 남기고 무시
            log.warn("[Redis] 잘못된 형식의 토큰 블랙리스트 등록 시도됨: {}", e.getMessage());
        } catch (Exception e) {
            // 그 외 알 수 없는 에러
            log.error("[Redis] 블랙리스트 등록 중 시스템 에러 발생: {}", e.getMessage());
        }
    }

    /**
     * 2. 해당 Token이 Redis의 블랙리스트에 존재하는지 확인하는 함수
     */
    public Boolean checkTokenInBlacklist(String token) {
        return redisTemplate.hasKey("BL:" + jwtUtil.getJtiFromToken(token));
    }

}
