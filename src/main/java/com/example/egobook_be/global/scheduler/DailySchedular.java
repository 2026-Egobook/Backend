package com.example.egobook_be.global.scheduler;

import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.friend.repository.FriendRequestRepository;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.shop.mapper.UserItemMapper;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySchedular {
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final UserTermRepository userTermRepository;
    private final DiaryRepository diaryRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PlazaLetterReplyReportRepository plazaLetterReplyReportRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final NotificationRepository notificationRepository;
    private final PlazaLetterRepository plazaLetterRepository;
    private final UserKnowledgeRepository userKnowledgeRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final InkLogRepository inkLogRepository;
    private final PlazaLetterThreadRepository plazaLetterThreadRepository;

    /**
     * 모든 사용자들의 일일 미션을 초기화하는 스케줄러 함수
     * - 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetAllDailyMissions(){
        log.info("🕛 [DailySchedular] 일일 미션 초기화 작업 시작");
        long startTime = System.currentTimeMillis();
        missionRepository.resetAllDailyMissions();
        long endTime = System.currentTimeMillis();
        log.info("🕛 [DailySchedular] 일일 미션 초기화 작업 종료. (소요시간: {}ms)", endTime-startTime);
    }

    /**
     * 모든 사용자들을 출석 보상을 받을 수 있는 상태로 변경해주는 스케줄러 함수
     * - 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetUserAttendanceStatus(){
        log.info("🕛 [DailySchedular] 사용자 일일 출석 보상 설정 작업 시작");
        long startTime = System.currentTimeMillis();
        userRepository.resetAllAttendancesStatus();
        long endTime = System.currentTimeMillis();
        log.info("🕛 [DailySchedular] 사용자 일일 출석 보상 설정 작업 종료. (소요시간: {}ms)", endTime-startTime);
    }

    /**
     * 사용자들 중 purgeAt이 지난 사용자 데이터들을 모두 삭제하는 스케줄러
     * - 매일 자정(00:00:00)에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeUsers() {
        log.info("🕛 [DailySchedular] 탈퇴한 사용자들의 정보들 일괄 삭제 작업 시작");
        long startTime = System.currentTimeMillis();

        // 1. 삭제 대상 조회 (삭제 예정일이 지났고, 상태가 WITHDRAW_PENDING인 유저)
        LocalDateTime now = LocalDateTime.now();
        List<User> targetUsers = userRepository.findByStatusAndPurgeAtBefore(UserStatus.WITHDRAW_PENDING, now);

        if (targetUsers.isEmpty()) {
            log.info("🕛 [DailySchedular] 삭제 대상이 없습니다. 작업을 종료합니다.");
            return;
        }

        // 2. 삭제 대상들 전부 삭제
        int count = targetUsers.size();
        deleteUsers(targetUsers);

        long endTime = System.currentTimeMillis();
        log.info("🕛 [DailySchedular] 총 {}명의 사용자 및 연관 데이터 삭제 완료. (소요시간: {}ms)", count, endTime - startTime);
    }

    /**
     * 사용자들과 연관된 데이터들을 모두 삭제하는 함수
     * [ 삭제 순서 ]
     * (0) Refresh Token Backup 테이블 삭제 (RefreshTokenBackup)
     * (1) 사용자 인증 정보 삭제 (AuthAccount)
     * (2) 사용자 약관 정보 삭제 (UserTerm)
     * (3) 사용자가 보유한 아이템 삭제 (UserItem)
     * (4) 사용자가 작성한 일기 삭제 (Diary)
     * (5) 사용자의 친구 연결 삭제 (Friend) -> 해당 친구가 기준 사용자인 레코드도 삭제
     * (6) 사용자가 보낸/받은 친구 연결 신청 삭제 (FriendRequest)
     * (7) 사용자의 미션 상태 정보 삭제 (Mission)
     * (8) 사용자가 신고한 편지 답장 정보 삭제 (PlazaLetterReplyReport)
     * (9) 사용자가 답장한 편지 정보에서 Replier(답장자) 비식별화 (PlazaLetterReply) -> Replier가 탈퇴하면 답장을 지우지 않고, PlazaLetterReply.replierId를 null로 만든다.
     * (10) 사용자가 작성한 편지 정보 삭제 (PlazaLetter) -> Thread는 삭제하지 않음.
     * (11) 사용자가 받은 알림 정보 삭제 (Notification)
     * (12) 사용자가 받은 오늘의 심리지식 정보들 삭제 (UserKnowledge)
     * (13) 사용자가 작성한 오늘의 질문에 대한 답변들 삭제 (QuestionAnswer)
     * (14) 사용자의 능력치 정보 삭제 (Ability)
     * (15) 사용자의 Ink 획득 기록 삭제 (InkLog)
     * (16) 최종 사용자 정보 삭제 (User)
     */
    private void deleteUsers(List<User> users) {
        refreshTokenBackupRepository.bulkDeleteByAuthAccountUserIn(users);
        // 1. 사용자 인증 정보 삭제 (AuthAccount)
        authAccountRepository.bulkDeleteByUserIn(users);
        // 2. 사용자 약관 정보 삭제
        userTermRepository.bulkDeleteByUserIn(users);
        // 3. 사용자가 보유한 아이템 삭제(Cascade로 처리됨)
        // 4. 사용자가 작성한 일기 삭제
        diaryRepository.bulkDeleteByUserIn(users);
        /*
         * 5. 사용자의 친구 연결 삭제 (해당 친구가 기준 사용자인 레코드도 삭제)
         * - 내가 user_id(기준 사용자)이거나, 내가 friend_id(친구)인 모든 데이터를 삭제한다.
         */
        friendRepository.bulkDeleteByUsersInOrFriendsIn(users, users);
        // 6. 해당 사용자가 보낸/받은 친구 연결 신청 삭제
        friendRequestRepository.bulkDeleteBySenderInOrReceiverIn(users, users);
        // 7. 사용자의 미션 상태 정보 삭제
        missionRepository.bulkDeleteByUserIn(users);
        /*
         * 8. 사용자가 신고한 편지 답장 정보 삭제
         * (1) 본인이 신고한(Reporter) 레코드만 삭제하고, 본인이 신고당한 레코드는 삭제하지 않는다.
         * (2) 본인이 신고당한(Replier) 레코드의 replierId값은 null로 설정해주어야 한다.
         */
        plazaLetterReplyReportRepository.bulkDeleteByReporterIdIn(users.stream().map(User::getId).toList());
        plazaLetterReplyReportRepository.bulkNullifyReplierId(users.stream().map(User::getId).toList());
        /*
         * 9. 사용자가 답장한 편지 정보
         * - 사용자가 답장한 편지 정보에서 Replier(답장자) 비식별화
         * - Replier가 탈퇴하면 답장을 지우지 않고, PlazaLetterReply.replierId를 null로 만든다
         * => replier가 null이 된 답변 객체는, 해당 편지 작성자가 회원탈퇴를 할 때 해당 사용자가 작성한 편지 정보들이 삭제되면서 같이 삭제된다.
         */
        plazaLetterReplyRepository.bulkNullifyReplierId(users.stream().map(User::getId).toList());

        /*
         * 10. 사용자가 작성한 편지 정보 비식별화 OR 삭제
         * (1) 탈퇴하는 user가 보낸(Sender) 편지의 senderId를 Null로 변경
         * (2) 탈퇴하는 user가 받은(Receiver) 편지의 receiverId를 null로 변경
         * (3) Sender, Receiver가 모두 null이 된(양쪽 다 탈퇴한) 편지는 DB에서 삭제
         * (4) 편지가 모두 삭제되어, 자식 객체들이 모두 사라져버린 Thread 삭제
         */
        plazaLetterRepository.bulkNullifySenderId(users.stream().map(User::getId).toList());
        plazaLetterRepository.bulkNullifyReceiverId(users.stream().map(User::getId).toList());
        plazaLetterRepository.bulkDeleteOrphanedLetters();
        plazaLetterThreadRepository.bulkDeleteEmptyThreads();


        // 11. 사용자가 받은 알림 정보 삭제
        notificationRepository.bulkDeleteByUserIn(users);
        // 12. 사용자가 받은 오늘의 심리지식 정보들 삭제
        userKnowledgeRepository.bulkDeleteByUserIn(users);
        // 13. 사용자가 작성한 오늘의 질문에 대한 답변들
        questionAnswerRepository.bulkDeleteByUserIn(users);
        // 14. 사용자의 능력치 정보 삭제 (Cascade로 처리됨)
        // 15. 사용자의 Ink 획득 기록 삭제
        inkLogRepository.bulkDeleteByUserIn(users);
        // 16. 최종 사용자 정보 삭제
        userRepository.deleteAll(users);
    }

}
