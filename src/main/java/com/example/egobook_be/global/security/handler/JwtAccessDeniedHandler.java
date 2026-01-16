package com.example.egobook_be.global.security.handler;

import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인은 성공했으나, 해당 리소스에 접근할 권한(Role)이 부족할 때 호출되는 클래스.
 * - ex) User가 Admin 페이지 접근
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;


    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 1. 로그 기록
        // 보안상 위험한 시도일 수 있으므로 사용자 식별 정보(IP 등)를 함께 남기는 것이 좋습니다. (여기선 기본 로그만)
        String requestURI = request.getRequestURI();
        log.warn("[권한 거부] 접근 시도 URL: {}, Error: {}", requestURI, accessDeniedException.getMessage());

        // 2. 응답 전송
        sendErrorResponse(response, GlobalErrorCode.ACCESS_DENIED.getStatus(), GlobalErrorCode.ACCESS_DENIED.getMessage());
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
