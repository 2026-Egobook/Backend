package com.example.egobook_be.global.security.handler;

import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 인증되지 않은 사용자(토큰 없음, 토큰 만료, 위조된 토큰)가 보호된 리소스에 접근할 때 호출됨(401 Unauthorized)
 * - Security Filter에 적용시킬 클래스
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper; // 직렬화 & 역직렬화를 수행해주는 클래스

    /**
     * Security Filter에서 적용시킬 함수. 미인증 사용자가 보호 리소스에 접근했을 때 실행시킬 함수이다.
     * @param request HttpServletRequest. 클라이언트의 요청
     * @param response HttpServletResponse. 클라이언트에게 보낼 응답
     * @param authException AuthenticationException. 접근 거부 예외 객체
     * @throws IOException 입출력 예외
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        /**
         * 1. 로그 기록
         * - 어떤 url 요청에서 어떤 오류가 발생했는지 서버 로그 찍기
         */
        String requestURI = request.getRequestURI();
        log.warn("[인증 실패] 접근 시도 URL: {}, Error: {}", requestURI, authException.getMessage());

        /**
         * 2. 응답 메시지 구성
         * - HttpServletResponse 객체에 반환 형식을 지정한다.
         */
        sendErrorResponse(response, GlobalErrorCode.UNAUTHORIZED.getStatus(), GlobalErrorCode.UNAUTHORIZED.getMessage());
    }

    /**
     * HttpServletResponse 객체에 status, contentType 등 반환할 Error 응답을 response에 구성하는 함수
     * @param response HttpServletResponse
     * @param status HttpStatus
     * @param message String
     * @throws IOException
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        GlobalResponse<?> errorResponse = GlobalResponse.error(status.value(), message);

        // ObjectMapper로 자바 객체->JSON 문자열로 직렬화한다. 이후, 문자 스트림(Writer)으로 전송한다. (ObjectMapper를 사용하면 자동으로 JSON 직렬화가 가능하다. 따라서 ResponseEntity가 클라이언트-서버간에 이동할 때도 내부적으로 ObjectMapper가 사용된다.)
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
