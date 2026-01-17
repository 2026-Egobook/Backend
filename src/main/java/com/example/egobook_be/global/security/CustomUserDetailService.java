package com.example.egobook_be.global.security;


import com.example.egobook_be.global.util.module.UserAuthDto;
import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.enums.Provider;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [사용자 정보를 로드하는 인터페이스]
 * DB에서 사용자를 조회 -> CustomUserDetails로 변환 -> Security로 반환
 * JwtAuthFilter는 UserDetailsService를 사용하여 사용자 정보를 가져온다.
 * => UserDetailService는 단순히 사용자 정보를 가져와서 CustomUserDetails로 변환해주는 인터페이스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final AuthAccountRepository authAccountRepository;

    /**
     * [provider, deviceUid를 사용해 DB를 직접 검색하여, User 인증 정보가 담긴 CustomUserDetails 객체를 반환하는 함수]
     * JwtAuthFilter가 해당 메서드를 호출해, DB 사용자를 로딩하여 UserDetails를 반환받는다.
     * @param compositeKey : "provider:deviceUid" 형식으로 되어있는 혼합 key
     * @return provider, deviceUid로 검색한 사용자 정보가 담긴 CustomUserDetails 객체
     */
    @Override
    @Transactional(readOnly = true) // readOnly 설정을 킴으로써 Dirty Checking 수행 안함
    public UserDetails loadUserByUsername(String compositeKey) {
        /*
         * 1. 전달받은 "provider:deviceUid" 형식의 혼합 키(Jwt에 지정된 Subject)를 ":" 기준으로 분리한다(문자열 파싱).
         * - 분리한 코드들을 각각 providerStr, deviceUid에 지정한다.
         *
         * throw: Jwt token에 있던 subject를 분리한 결과의 길이가 2 미만이면 예외
         */
        String[] seperated =  compositeKey.split(":", 2);
        if (seperated.length < 2) {
            throw new CustomException(AuthErrorCode.INVALID_TYPE_TOKEN);
        }
        String providerStr = seperated[0];
        String deviceUid = seperated[1];

        /*
         * 2. providerStr을 Provider Enum Class로 변환한다.
         * - Provider 내부에 선언되어있는 resolve 함수를 사용하여, 해당 문자열이 Provider 내부에 존재하는 Enum인지 확인한다.
         * - 존재한다면 Provider 객체를, 없다면 null을 반환한다.
         */
        Provider provider = Provider.resolve(providerStr);
        if(provider==null){
            throw new CustomException(AuthErrorCode.INVALID_PROVIDER);
        }

        /*
         * 3. deviceUid, AuthAccountRepository로 기기 정보를 조회한다.
         * - AuthAccountRepository에서 fetch join으로 영속성 컨텍스트에 User 정보까지 같이 가져온 상태이다.
         * throw 해당 UID 기기를 찾을 수 없다는 예외
         */
        AuthAccount authAccount = authAccountRepository.findByDeviceUidAndProvider(deviceUid, provider)
                .orElseThrow(() -> new CustomException(AuthErrorCode.UID_NOT_FOUND));

        /*
         * 4. 해당 인증 데이터가 User와 연결되어있는지 확인한다.
         * - User가 연결 안되어있는 고아객체인지 확인
         * throw AuthAccount에 연결된 user가 누락되었다는 예외
         */
        User user = authAccount.getUser();
        if(user == null){
            log.error("치명적 오류: AuthAccount(id={})에 연결된 User 데이터가 누락되었습니다.", authAccount.getId());
            throw new CustomException(AuthErrorCode.AUTH_ACCOUNT_USER_MISSING);
        }

        /*
         * 5. Entity -> DTO 변환
         * - 인증 객체(CustomUserDetails)에 엔티티를 직접 넣지 않고, 필요한 데이터만 담은 DTO로 변환하여 주입한다.
         * - 위에서 영속성 컨텍스트로 AuthAccount에 User도 같이 가져왔기에, N+1 문제가 발생하지 않는다.
         */
        UserAuthDto userAuthDto = UserAuthDto.builder()
                .userId(user.getId())           // User 테이블의 PK (비즈니스 로직용)
                .authAccountId(authAccount.getId()) // AuthAccount 테이블의 PK (토큰 백업용)
                .provider(authAccount.getProvider()) // provider 설정
                .hashedDeviceUid(authAccount.getHashedDeviceUid()) // hashing된 기기 고유 ID
                .role(user.getRole())           // 사용자 권한 (RoleType)
                .build();

        /*
         * 6. CustomUserDetails 생성 후 반환
         */
        return new CustomUserDetails(userAuthDto);
    }
}
