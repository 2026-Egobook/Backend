package com.example.egobook_be.domain.home.service;

import com.example.egobook_be.domain.home.mapper.HomeMapper;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.util.InkLogUtil;
import org.aspectj.lang.annotation.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@Transactional // -> 아래 테스트에 @Transactional을 걸면, Main Thread에서 해당 트랜잭션이 끝날 때까지 User Raw에 대한 락을 갖고 있기 때문에, 내부에서 생성된 자식 Thread에서 해당 User Raw에 대한 접근권을 갖지 못한다.
// 따라서 해당 Raw에 대한 Lock 대기 시간이 초과되어 "Lock wait timeout exceeded" 에러가 뜨게 된다.
// @BeforeEach, @Test, @AfterEach는 하나의 트랜잭션으로 묶여버린다.
@ActiveProfiles( "test") // application-test.yml
public class HomeServiceIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private InkLogRepository inkLogRepository;
    @Autowired private HomeService homeService;

    private Long userId;

    @BeforeEach
    public void setup() {
        User user = User.builder()
                .accountCode("test")
                .email("test@example.com")
                .nickname("testNickname")
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .ink(10)
                .build();
        userRepository.save(user);
        this.userId = user.getId();
    }

    @Test
    @DisplayName("[성공] 홈 화면에 접속해서 출석 보상 수령 - 2개의 Thread에서 동시에 API 호출")
    void successGetDailyRewardWithTwoThread() throws InterruptedException {
        // =========== Given ===========
        int threadCount = 2; // 실행할 스레드 개수
        // 1. 카운트다운 래치(대기표) 생성 - 모든 Thread의 작업이 끝날 때까지 기다리기 위한 장치 (인자로 정해준 카운터가 0으로 소모되어야 다음 작업이 실행됨)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 2. 실행할 스레드만큼의 크기로 Thread Pool 생성
        try(ExecutorService executorService = Executors.newFixedThreadPool(threadCount)){
            // =========== When ===========
            // 1. 생성한 Thread Pool에 속한 각 Thread에게 작업 부여
            for(int i=0; i<threadCount; i++){
                // 2. 각 Thread에게 작업 부여
                // executorService.submit(): 스레드 풀에 작업을 제출할 때 사용하는 함수.
                // 작업을 Queue에 넣고 바로 Future 객체를 반환한다.
                executorService.submit(() -> {
                    try {
                        homeService.getHomeData(userId);
                    } catch (Exception e) {
                        e.printStackTrace(); // 예외 발생 시 로그 찍기
                    } finally {
                        // finally를 통해 작업이 끝나면 바로 latch의 카운트를 감소시킨다.
                        latch.countDown();
                    }
                });
            }
            // 2. 각 Thread의 작업이 끝날 때까지 대기
            latch.await();
            executorService.shutdown();
        } // executorService.shutdown()이 자동으로 호출된다.


        // =========== Then ===========
        // 결과 확인을 위해, DB에서 최신 User를 조회
        User updatedUser = userRepository.findById(userId).orElseThrow();

        // [검증 1] 출석 보상이 1번만 부여되었는지 검증
        assertThat(updatedUser.getInk()).isEqualTo(13);

        // [검증 2] 출석 보상으로 InkLog가 1개만 생성되어있는지 검증
        int logCount = inkLogRepository.countByUser(updatedUser);
        assertThat(logCount).isEqualTo(1);
    }

    @AfterEach
    void tearDown(){
        inkLogRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
