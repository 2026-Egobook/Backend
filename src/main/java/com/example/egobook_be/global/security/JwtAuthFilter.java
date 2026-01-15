package com.example.egobook_be.global.security;

import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.global.enums.JwtTokenType;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [JWT 인증 필터]
 * 클라이언트 요청 시 헤더에 담긴 JWT(Access Token)을 검증하고,
 * 유효한 경우 사용자 인증 객체(Authentication)를 생성하여 SecurityContext에 저장한다.
 * - OncePerRequestFilter: 모든 요청에 대해 단 한 번만 실행됨을 보장한다.
 * => 이 필터는 "Access Token"에 대한 검증만 수행한다. 따라서, Refresh, Recover Token에 대한 처리는 AuthController가 담당할 것이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailService customUserDetailService;
    private final AuthenticationEntryPoint authenticationEntryPoint; // 예외 처리를 위한 클래스
    private static final String AUTHORIZATION_HEADER = "Authorization"; // HTTP 헤더 키
    private static final String BEARER_PREFIX = "Bearer "; // 토큰 접두사

    /**
     * [JWT Access Token 검증 필터 수행 함수]
     * - JwtAuthFilter 자체적으로 사용자를 검증한다
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        /**
         * 1. Request Header에서 토큰 추출
         * - Authorization Header에서 토큰만 추출한다.
         */
        String token = resolveToken(request);

        /**
         * 토큰이 없다면 바로 다음 필터로 넘김 (비로그인 허용 엔드포인트 등을 위해)
         */
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            /**
             * 2. 토큰 유효성 검증 (위조, 만료 등 확인)
             */
            if (jwtUtil.validateToken(token)) {
                /**
                 * 3. JwtTokenType을 가져와 Access Token인지 확인한다.
                 * - Refresh Token으로는 API 접근이 불가하도록 설정한다.
                 * - Access Token이 아닌 다른 토큰(Refresh, Recover)으로 요청이 왔다면 에러 로그 & 예외를 발생시킨다.
                 */
                JwtTokenType type = jwtUtil.getTokenType(token);
                if (!JwtTokenType.ACCESS.equals(type)) {
                    log.warn("Access Token이 아닌 토큰으로 접근 시도. Type: {}", type);
                    throw new CustomException(AuthErrorCode.ACCESS_WITH_NON_ACCESS_TYPE_TOKEN);
                }

                /**
                 * 4. Access 토큰에서 사용자 식별자(Subject) 추출 (형식: "provider:deviceUid")
                 */
                String compositeKey = jwtUtil.getSubjectFromToken(token);

                /**
                 * 5. CustomUserDetailService를 통해 유저 정보(UserDetails) 로드
                 */
                UserDetails userDetails = customUserDetailService.loadUserByUsername(compositeKey);

                /**
                 * 6. 인증 객체(Authentication) 생성
                 * UsernamePasswordAuthenticationToken: Spring Security 표준 인증 객체
                 * 비밀번호(credentials)는 이미 토큰 검증으로 대체되었으므로 null 처리
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                /**
                 * 7. SecurityContext에 인증 객체 저장 (로그인 처리 완료)
                 * - SecurityContext에 저장된 해당 인증 객체는 한번의 요청에서 사용되고 삭제된다.
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", compositeKey);

                // 7. 다음 필터로 요청 전달
                filterChain.doFilter(request, response);
            }
        } catch (CustomException e) {
            log.error("인증 처리 중 비즈니스 예외 발생: {}", e.getMessage());
            request.setAttribute("exception", e); // EntryPoint에서 처리하도록 속성 저장
            authenticationEntryPoint.commence(request, response, new AuthenticationException(e.getMessage(), e) {});
        } catch (Exception e) {
            log.error("인증 필터 내부 시스템 오류: {}", e.getMessage());
            request.setAttribute("exception", e);
            authenticationEntryPoint.commence(request, response, new AuthenticationException(e.getMessage(), e) {});
        }
    }

    /**
     * [Http Request Header에서 토큰 정보를 꺼내오는 메서드]
     * - Authorization 헤더 값을 추출한다.
     * - 클라이언트가 요청할 때, Authorization 헤더는 아래처럼 온다
     *      -> "Authorization: Bearer eyJhbGci..."
     * - "Bearer " 부분은 인증 스키마이기 때문에, 해당 부분을 자르고 순수한 JWT 문자열을 구하기 위해 substring을 하여 토큰 부분만 추출한다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7); // "Bearer " 이후의 문자열만 반환
        }
        return null;
    }



}
