package com.example.egobook_be.domain.letters.service.scheduler;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterWaitingDispatchService {

    private final PlazaLetterRepository plazaLetterRepository;
    private final UserRepository userRepository;

    // 유저 1명당 편지 1건씩만 배정 (원하면 batchSize 조절)
    private static final int BATCH_SIZE = 50;

    @Transactional
    public int dispatchWaitingLetters() {
        OffsetDateTime now = OffsetDateTime.now();

        // 1) 수신 가능 유저 찾기 (포기 후 4시간 지난 유저)
        List<Long> candidateUserIds =
                userRepository.findReceivableUsers(now, PageRequest.of(0, BATCH_SIZE)); // 이 부분은 PageRequest를 Pageable로 사용하고 있음

        if (candidateUserIds.isEmpty()) return 0;

        // 2) WAITING 편지 중 아직 receiver가 없는 편지 가져오기 (포기된 편지 제외는 repo 쿼리에서 처리)
        List<PlazaLetter> waitingLetters =
                plazaLetterRepository.findWaitingLetters(PageRequest.of(0, candidateUserIds.size())); // 동일하게 PageRequest를 사용

        if (waitingLetters.isEmpty()) return 0;

        int dispatchCount = Math.min(candidateUserIds.size(), waitingLetters.size());

        for (int i = 0; i < dispatchCount; i++) {
            PlazaLetter letter = waitingLetters.get(i);
            Long receiverId = candidateUserIds.get(i);

            // 편지에 수신자 붙이고 ARRIVED로 전환 + arrivedAt/replyDeadlineAt 설정
            letter.assignToReceiver(receiverId, now, now.plusHours(24));

            // 유저는 이제 다시 차단 해제 상태(원하면 null로 지우거나 유지 가능)
            // 이 부분은 정책 선택: "차단이 끝나면 수신 가능"만 필요하면 굳이 null로 안 지워도 됨.
        }

        return dispatchCount;
    }
}
