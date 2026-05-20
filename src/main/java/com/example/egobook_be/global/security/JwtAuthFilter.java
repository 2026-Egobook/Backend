package com.example.egobook_be.global.security;

import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.global.enums.JwtTokenType;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.JwtUtil;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.module.UserAuthDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RedisUtil redisUtil;
    private final CustomUserDetailService customUserDetailService;
    private final AuthenticationEntryPoint authenticationEntryPoint; // 예외 처리를 위한 클래스
    private static final String AUTHORIZATION_HEADER = "Authorization"; // HTTP 헤더 키
    private static final String BEARER_PREFIX = "Bearer "; // 토큰 접두사

    // ======================================================================
    // [Protected]
    // ======================================================================
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

        /*
         * 1. Request Header에서 토큰 추출
         * - Authorization Header에서 토큰만 추출한다.
         */
        String token = resolveToken(request);

        // 토큰이 없다면 바로 다음 필터로 넘김 (비로그인 허용 엔드포인트 등을 위해)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            /*
             * 3. JwtTokenType을 가져와 Access Token인지 확인한다.
             * - Refresh Token으로는 API 접근이 불가하도록 설정한다.
             * - Access Token이 아닌 다른 토큰(Refresh, Recover)으로 요청이 왔다면 에러 로그 & 예외를 발생시킨다.
             */
            JwtTokenType type = jwtUtil.getTokenType(token);
            if (!JwtTokenType.ACCESS.equals(type)) {
                log.warn("Access Token이 아닌 토큰으로 접근 시도. Type: {}", type);
                throw new CustomException(AuthErrorCode.ACCESS_WITH_NON_ACCESS_TYPE_TOKEN);
            }

            // 4. 해당 Access Token이 Redis의 블랙리스트에 존재하는지 확인한다.
            if(redisUtil.checkTokenInBlacklist(token)){
                log.warn("[Redis] BlackList에 등록된 Access Token으로 접근이 시도되었습니다. Token: {}", token);
                throw new CustomException(AuthErrorCode.BLACKLISTED_TOKEN);
            }

            // 5. DB 조회 없이 토큰 정보만으로 인증 객체 생성
            Authentication authentication = createAuthentication(token);

            // 6. SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 인증 정보를 저장했습니다: {}", authentication.getName());
        } catch (CustomException e) {
            log.error("인증 처리 중 비즈니스 예외 발생: {}", e.getMessage());
            request.setAttribute("exception", e); // EntryPoint에서 처리하도록 속성 저장
            authenticationEntryPoint.commence(request, response, new AuthenticationException(e.getMessage(), e) {});
            return; // 여기서 끝내야지, 아래의 doFilter가 실행되지 않음 (Double Response 방지)
        } catch (Exception e) {
            log.error("인증 필터 내부 시스템 오류: {}", e.getMessage());
            request.setAttribute("exception", e);
            authenticationEntryPoint.commence(request, response, new AuthenticationException(e.getMessage(), e) {});
            return; // 여기서 끝내야지, 아래의 doFilter가 실행되지 않음 (Double Response 방지)
        }
        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }


    // ======================================================================
    // [Private]
    // ======================================================================
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

    /**
     * [Authentication 객체 생성 메서드]
     * DB를 거치지 않고 JWT Claims에서 정보를 추출하여 Principal을 구성한다.
     * Admin과 User를 subject 접두사로 분기하여 처리한다.
     * @param accessToken : 클라이언트로부터 받은 Access Token
     * @return Authentication 객체
     */
    private Authentication createAuthentication(String accessToken) {
        // 1. Access Token에서 데이터 추출
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        Long authAccountId = jwtUtil.getAuthAccountIdFromToken(accessToken);
        String subject = jwtUtil.getSubjectFromToken(accessToken);
        String roleString = jwtUtil.getRoleFromToken(accessToken);

        if (subject == null) {
            throw new CustomException(AuthErrorCode.INVALID_TYPE_TOKEN);
        }

        // 2. Admin 분기: subject가 "ADMIN:"으로 시작
        if (subject.startsWith("ADMIN:")) {
            UserAuthDto userAuthDto = UserAuthDto.builder()
                    .adminId(userId)
                    .subject(subject)
                    .role(RoleType.valueOf(roleString))
                    .build();
            CustomUserDetails customUserDetails = new CustomUserDetails(userAuthDto);
            return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        }

        // 3. User 분기: 기존 로직 유지
        String[] parts = subject.split(":", 2);
        if (parts.length < 2) {
            throw new CustomException(AuthErrorCode.INVALID_TYPE_TOKEN);
        }
        Provider provider = Provider.valueOf(parts[0]);
        String hashedDeviceUid = parts[1];

        // 4. UserAuthDto & CustomUserDetails 생성
        UserAuthDto userAuthDto = UserAuthDto.builder()
                .userId(userId)
                .authAccountId(authAccountId)
                .hashedDeviceUid(hashedDeviceUid)
                .provider(provider)
                .subject(subject)
                .role(RoleType.valueOf(roleString))
                .build();
        CustomUserDetails customUserDetails = new CustomUserDetails(userAuthDto);

        // 5. Spring Security 인증 토큰 생성 및 반환
        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }


}
