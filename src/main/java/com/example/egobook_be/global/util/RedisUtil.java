package com.example.egobook_be.global.util;

import com.example.egobook_be.global.util.module.RedisValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

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

    /**
     * 1. Refresh Token 정보 저장 (Set)
     * - Key: "RT:{hashedRefreshToken}" (Prefix를 붙여 관리 용이성 확보)
     * - Value: Value로 들어갈 Json 형태의 데이터들
     * - TTL: 만료 시간까지 남은 시간만큼 설정
     */
    public void setRefreshToken(String hashedRefreshToken, RedisValue value, long durationInMillis) {
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
    public RedisValue getRefreshToken(String hashedRefreshToken) {
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
    public void deleteRefreshToken(String hashedRefreshToken) {
        String key = "RT:" + hashedRefreshToken;
        redisTemplate.delete(key); //
    }

    /**
     * 4. 해당 RefreshToken 존재 여부 확인
     */
    public boolean hasKey(String hashedRefreshToken) {
        return redisTemplate.hasKey("RT:" + hashedRefreshToken);
    }
}
