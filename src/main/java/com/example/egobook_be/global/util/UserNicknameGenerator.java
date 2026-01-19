package com.example.egobook_be.global.util;

import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 사용자의 닉네임을 자동으로 생성해주는 클래스
 * 기본 형식: 에고북 + 숫자 4개 (에고북0042)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserNicknameGenerator {
    private static final String PREFIX = "에고북"; // 닉네임에 공통으로 들어갈 접두사
    private static final int MAX_RETRY_COUNT = 100; // 최대 닉네임 생성 시도 횟수

    private final UserRepository userRepository;
    private final Random random = new Random();

    /**
     * Unique한 닉네임을 생성하는 함수 (에고북 + 4자리 숫자)
     * - MAX_RETRY_COUNT만큼 닉네임 생성을 시도하는데, 이때 해당 횟수 이상으로 닉네임 생성일 실패하면 에러 서버 로그를 발생시킨다.
     * @return
     */
    public String generateUniqueNickname() {
        String nickname;
        int retryCount = 0;

        do {
            int randomNum = random.nextInt(10000); // 0 ~ 9999 사이의 난수 생성
            nickname = String.format("%s%04d", PREFIX, randomNum); // 포맷팅: "에고북0001", "에고북9999"
            retryCount++;
            if (retryCount > MAX_RETRY_COUNT) {
                log.error("[Class:UserNicknameGenerator] 닉네임 생성 실패: {}번의 닉네임 생성 시도를 하였지만 실패하였습니다. 랜덤 닉네임 생성 범위를 증가시켜야합니다.", MAX_RETRY_COUNT);
                throw new CustomException(AuthErrorCode.NICKNAME_GENERATE_FAILED);
            }
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }



}
