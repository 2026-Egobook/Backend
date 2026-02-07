package com.example.egobook_be.domain.letters.service.scheduler;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterGiveUpService {

    private final PlazaLetterRepository plazaLetterRepository;
    private final UserRepository userRepository;

    @Transactional
    public int autoGiveUpExpiredLetters() {
        OffsetDateTime now = OffsetDateTime.now();

        PlazaLetterStatus arrived = PlazaLetterStatus.ARRIVED;
        PlazaLetterStatus deferred = PlazaLetterStatus.DEFERRED;
        Pageable pageable = PageRequest.of(0, 200);


        List<PlazaLetter> targets = plazaLetterRepository.findGiveUpTargets(now, arrived, deferred, pageable);

        if (targets.isEmpty()) return 0;

        for (PlazaLetter letter : targets) {

            letter.markGaveUp(now);

            Long receiverId = letter.getReceiverId();
            if (receiverId != null) {
                User receiver = userRepository.findById(receiverId).orElse(null);
                if (receiver != null) {
                    receiver.blockLetterReceiveUntil(now.plusHours(4));
                }
            }
        }
        return targets.size();
    }
}
