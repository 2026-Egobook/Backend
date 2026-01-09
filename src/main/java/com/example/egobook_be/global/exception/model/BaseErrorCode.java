package com.example.egobook_be.global.exception.model;

import org.springframework.http.HttpStatus;

/**
 * CustomException에 들어갈 기본적인 클래스의 인터페이스
 * 각 도메인별로 예외 Enum Class들을 선언할 때, 해당 Enum 클래스들의 부모 인터페이스
*/
public interface BaseErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getStatus();
}
