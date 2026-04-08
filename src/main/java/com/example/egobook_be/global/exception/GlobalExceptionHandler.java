package com.example.egobook_be.global.exception;

import com.example.egobook_be.global.exception.model.BaseErrorCode;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException e){
        // 1. 함수 인자로 받은 CustomException의 멤버변수인 "BaseErrorCode"를 상속받은 Enum Class를 "baseErrorCode" 변수로 받는다.
        BaseErrorCode baseErrorCode = e.getErrorCode();

        // 2. Custom 오류가 발생했다는 로그와 해당 예외로 설정해둔 Error Code를 로깅한다.
        log.error("Custom 오류 발생: {}", e.getErrorCode());


        // 3. ResponseEntity 객체에 baseErrorCode의 status를 설정해주고, body에는 GlobalResponse 클래스의 "error()" 함수로 error 내용을 담은 객체를 넣어준다.
        return ResponseEntity
                .status(baseErrorCode.getStatus()) // ResponseEntity에 Http status 설정
                .body(GlobalResponse.error(baseErrorCode.getStatus().value(), baseErrorCode.getMessage()));
    }

    // 비즈니스 로직 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<?>> handleIllegalArgumentException(IllegalArgumentException e){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(400, e.getMessage()));
    }

    // MethodArgumentNotValidException: 함수 인자의 @Valid를 할 때, 맞지 않는 형식이 있을 때 발생하는 에러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(400, e.getMessage()));
    }

    // MissingServletRequestParameterException: required=true 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GlobalResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(400, e.getMessage()));
    }

    // 파라미터 타입 불일치 (enum에 없는 값 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {}", e.getMessage());
        String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다.", e.getName(), e.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(400, message));
    }

    // 지원하지 않는 HTTP 메서드 (GET인데 POST 요청 등)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GlobalResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(GlobalResponse.error(405, "지원하지 않는 HTTP 메서드입니다: " + e.getMethod()));
    }

    // 지원하지 않는 Content-Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GlobalResponse<?>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(GlobalResponse.error(415, "지원하지 않는 Content-Type입니다: " + e.getContentType()));
    }

    // 존재하지 않는 URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponse<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("존재하지 않는 URL: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(GlobalResponse.error(404, "요청한 리소스를 찾을 수 없습니다."));
    }

    // Exception 최후의 보루 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleException(Exception e){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(500, e.getMessage()));
    }


}
