package com.example.egobook_be.global.exception;

import com.example.egobook_be.domain.ego_room.exception.SubscriptionLockedException;
import com.example.egobook_be.global.exception.model.BaseErrorCode;
import com.example.egobook_be.global.response.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        // 미구독시 cta 제공 위한 처리
        if (e instanceof SubscriptionLockedException) {
            SubscriptionLockedException subEx = (SubscriptionLockedException) e;


            Map<String, Object> body = new java.util.LinkedHashMap<>();

            body.put("status", baseErrorCode.getStatus().value());
            body.put("code", subEx.getDynamicCode());
            body.put("message", baseErrorCode.getMessage());
            body.put("result", subEx.getResult()); // CTA 정보 포함

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

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

    // Exception 최후의 보루 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleException(Exception e){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(500, e.getMessage()));
    }


}
