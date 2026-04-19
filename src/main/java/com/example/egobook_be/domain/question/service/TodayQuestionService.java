package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.mapper.MyAnswerHistoryMapper;
import com.example.egobook_be.domain.question.mapper.PublicAnswerMapper;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.restriction.service.RestrictionGuardService;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import com.example.egobook_be.global.util.InkLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodayQuestionService {

    private final TodayQuestionRepository todayQuestionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final InkLogRepository inkLogRepository;
    private final AbilityRepository abilityRepository;
    private final FriendRepository friendRepository;
    private final InkLogUtil inkLogUtil;
    private final RestrictionGuardService restrictionGuardService;

    /** 오늘의 질문 조회 **/
    @Transactional(readOnly = true)
    public TodayQuestionResDto getTodayQuestion(Long userId) {
        log.info("[TodayQuestionService] getTodayQuestion Start - userId: {}", userId);

        TodayQuestion question = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        boolean answered = false;
        MyTodayAnswerResDto myAnswer = null;

        if (userId != null) {
            Optional<QuestionAnswer> answerOpt =
                    questionAnswerRepository.findByUserIdAndQuestionIdWithQuestion(
                            userId,
                            question.getId()
                    );

            if (answerOpt.isPresent()) {
                answered = true;
                QuestionAnswer answer = answerOpt.get();

                myAnswer = MyTodayAnswerResDto.builder()
                        .answerId(answer.getId())
                        .content(answer.getContent())
                        .visibility(answer.getVisibility())
                        .answeredAt(answer.getCreatedAt())
                        .build();
            }
        }

        log.info("[TodayQuestionService] getTodayQuestion End - userId: {}", userId);
        return TodayQuestionResDto.builder()
                .questionId(question.getId())
                .content(question.getContent())
                .date(question.getQuestionDate())
                .answered(answered)
                .myAnswer(myAnswer)
                .build();
    }

    /** 답변 작성 **/
    @Transactional
    public void createAnswer(Long userId, AnswerCreateReqDto reqDto) {
        log.info("[TodayQuestionService] createAnswer Start - userId: {}", userId);
        // 1. User, Mission, Ability 객체 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Mission userMission = missionRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.MISSION_NOT_FOUND));
        Ability userAbility = abilityRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.ABILITY_NOT_FOUND));

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        if (questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)) {
            throw new CustomException(QuestionErrorCode.ALREADY_ANSWERED_TODAY);
        }

        restrictionGuardService.checkQuestionAnswerRestriction(userId);

        // 오늘 처음 "오늘의 질문"에 답변했는지 여부 확인
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime startOfDay = today.atStartOfDay(zoneId).toLocalDateTime();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(zoneId).toLocalDateTime();
        boolean isFirstAnswerToday = !questionAnswerRepository.existsByUserAndCreatedAtBetween(user, startOfDay, endOfDay);

        // 해당 사용자의 오늘의 답변 저장
        questionAnswerRepository.save(
                QuestionAnswer.builder()
                        .user(user)
                        .question(todayQuestion)
                        .content(reqDto.content())
                        .visibility(reqDto.visibility())
                        .build()
        );

        // =============================================
        // [ 보상 로직 ]
        // =============================================
        /*
         * 1. 해당 사용자가 오늘 처음 오늘의 질문에 답변했을 경우
         * - 잉크 +1
         * - 잉크 로그 작성
         * - 일일 미션 상태 업데이트
         * - 성실성 +1
         * + 레벨업 시 잉크 +1
         */
        List<InkLog> inkLogs = new ArrayList<>();
        if(isFirstAnswerToday){
            inkLogUtil.addInkLogToList(inkLogs, user, 1, InkLogType.FIRST_QUESTION_ANSWER); // 잉크 +1
            // 1-1. 만약 이번이 처음 일일 미션을 수행한 경우일 때
            if(userMission.updateDailyQuestionMissionStatus(true)){
                inkLogUtil.addInkLogToList(inkLogs, user, 1, InkLogType.DAILY_MISSION_REWARD);

                /*
                 * 1-2. 만약 오늘이 일일 미션을 7일째 완료한 날인 경우
                 * - 잉크 +2
                 * - 잉크 로그 추가
                 */
                if(userMission.isWeeklyMissionCompleted()){
                    inkLogUtil.addInkLogToList(inkLogs, user, 2, InkLogType.WEEKLY_MISSION_REWARD);
                }
            }
            int earnedInk = userAbility.addDiligence(1); // 성실성 +1
            // 1-3. 성실성 레벨업했는지 여부 확인
            if(earnedInk == 1){
                inkLogUtil.addInkLogToList(inkLogs, user, earnedInk, InkLogType.LEVEL_UP);
                user.levelUp();
            }
        }
        inkLogRepository.saveAll(inkLogs);
        log.info("[TodayQuestionService] createAnswer End - userId: {}", userId);
    }

    /** 오늘의 질문 PUBLIC 답변 전체 조회 **/
    @Transactional(readOnly = true)
    public SliceResponse<PublicAnswerResDto> getPublicAnswers(int page, int size) {
        log.info("[TodayQuestionService] getPublicAnswers Start");

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        PageRequest pageable = PageRequest.of(
                page -1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Slice<QuestionAnswer> slice =
                questionAnswerRepository.findPublicAnswersWithUser(
                        todayQuestion,
                        AnswerVisibility.PUBLIC,
                        pageable
                );

        log.info("[TodayQuestionService] getPublicAnswers End");
        return SliceResponse.of(slice, PublicAnswerMapper::toDto);
    }

    /** 답변 수정 **/
    @Transactional
    public void updateAnswer(Long userId, AnswerUpdateReqDto reqDto) {
        log.info("[TodayQuestionService] updateAnswer Start - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        QuestionAnswer answer = questionAnswerRepository
                .findByUserAndQuestion(user, todayQuestion)
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND)
                );

        restrictionGuardService.checkQuestionAnswerRestriction(userId);

        answer.update(
                reqDto.content(),
                reqDto.visibility()
        );
        log.info("[TodayQuestionService] updateAnswer End - userId: {}", userId);
    }

    /** 친구 답변 조회 **/
    @Transactional(readOnly = true)
    public SliceResponse<FriendAnswerResDto> getFriendsAnswers(
            Long userId,
            int page,
            int size
    ) {
        log.info("[TodayQuestionService] getFriendsAnswers Start - userId: {}", userId);
        User me = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        // 친구 ID만 조회
        List<Long> friendIds = friendRepository.findFriendIdsByUser(me);

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 친구 없으면 빈 Slice 반환
        if (friendIds.isEmpty()) {
            Slice<FriendAnswerResDto> emptySlice =
                    new SliceImpl<>(List.of(), pageable, false);

            return SliceResponse.of(emptySlice);
        }

        Slice<FriendAnswerResDto> slice =
                questionAnswerRepository.findFriendsAnswersSlice(
                        todayQuestion,
                        List.of(
                                AnswerVisibility.PUBLIC,
                                AnswerVisibility.FRIEND
                        ),
                        friendIds,
                        pageable
                );

        log.info("[TodayQuestionService] getFriendsAnswers End - userId: {}", userId);
        return SliceResponse.of(slice);
    }

    /** 내가 지금까지 작성한 모든 답변 조회 **/
    @Transactional(readOnly = true)
    public SliceResponse<MyAnswerHistoryResDto> getMyAnswerHistory(
            Long userId,
            int page,
            int size
    ) {
        log.info("[TodayQuestionService] getMyAnswerHistory Start - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        Pageable pageable = PageRequest.of(
                page -1 ,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Slice<QuestionAnswer> slice =
                questionAnswerRepository.findMyAnswerHistorySlice(user, pageable);

        log.info("[TodayQuestionService] getMyAnswerHistory End - userId: {}", userId);
        return SliceResponse.of(slice, MyAnswerHistoryMapper::toDto);
    }

    /** 내가 작성한 오늘의 질문 답변 삭제 **/
    @Transactional
    public void deleteAnswer(Long userId, Long answerId) {
        log.info("[TodayQuestionService] deleteAnswer Start - userId: {}, answerId: {}", userId, answerId);
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        QuestionAnswer answer = questionAnswerRepository
                .findByIdAndUser(answerId, user)
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND)
                );

        restrictionGuardService.checkQuestionAnswerRestriction(userId);

        questionAnswerRepository.delete(answer);
        log.info("[TodayQuestionService] deleteAnswer End - userId: {}, answerId: {}", userId, answerId);
    }
}
