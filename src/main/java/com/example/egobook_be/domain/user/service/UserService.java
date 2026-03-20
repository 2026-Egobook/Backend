package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import com.example.egobook_be.domain.auth.enums.AuthErrorCode;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.auth.repository.RefreshTokenBackupRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.shop.entity.Item;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ShopErrorCode;
import com.example.egobook_be.domain.shop.repository.ItemRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.entity.UserTerm;
import com.example.egobook_be.domain.terms.enums.TermErrorCode;
import com.example.egobook_be.domain.terms.repository.TermRepository;
import com.example.egobook_be.domain.terms.repository.UserTermRepository;
import com.example.egobook_be.domain.user.dto.FcmTokenReqDto;
import com.example.egobook_be.domain.user.dto.UserNicknameResDto;
import com.example.egobook_be.domain.user.dto.UserNicknameUpdateReqDto;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.AccountCodeGenerator;
import com.example.egobook_be.global.util.RedisUtil;
import com.example.egobook_be.global.util.UserNicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final ItemRepository itemRepository;
    private final TermRepository termRepository;
    private final MissionRepository missionRepository;
    private final UserTermRepository userTermRepository;
    private final UserItemRepository userItemRepository;
    private final AbilityRepository abilityRepository;
    private final RefreshTokenBackupRepository refreshTokenBackupRepository;
    private final RedisUtil redisUtil;
    private final UserNicknameGenerator userNicknameGenerator;

    @Value("${app.data.purge-duration-in-ms}")
    private Long purgeDurationInMs;

    /**
     * User를 생성하고 기본적인 세팅을 수행한 후 DB에 저장해주는 함수 (AuthAccount는 예외이다)
     * @param email 구글 이메일 -> 선택적으로 넣을 수 있음 (Google,Guest 사용자 둘 다 생성 가능)
     * @return 기본적인 초기 User 데이터와 연관 데이터들이 초기화된 User 객체
     */
    @Transactional
    public User initializeAndRegisterUser(String email){
        // 1. User Entity 생성
        User user = createUser(email);

        // 2. 초기 User 연관 데이터 생성
        allocateUser(user);
        return userRepository.save(user);
    }

    /**
     * User Entity 생성 공통 로직
     * - AccountCode, Nickname을 자동으로 생성해준다.
     * - email은 선택적으로 넣을 수 있다.
     * - User 생성 후, userRepository에 save()까지 수행한 결과물을 반환한다.
     * @param email : Guest-null, Google-Token에 있는 Google Email 설정
     */
    private User createUser(String email){
        /*
         * AuthAccount -> User Entity의 연관관계 설정을 위해, UserRepository로 먼저 save한다.
         *  (1) accountCode: 랜덤으로 생성된 공개 고유 ID 지정 (중복 여부 확인)
         *  (2) role : Builder.Default로 기본값 ROLE_USER 자동 지정
         *  (3) nickname : AccountCodeGenerator에서 자동으로 닉네임 생성 (DB에서 중복 체크도 함)
         *  (4) status: Builder.Default로 기본값 UserStatus.NEW 지정
         *  (5) email: Guest에서는 생성할 때 email을 지정 안한다. 나중에 사용자가 구글로 로그인하였을 때 채워진다.
         *  (6) streakCount: 연속 출석 수. 생성할 때 초기화되는 0으로 그대로 둔다.
         *  (7) lastLoginAt: 처음 회원가입 하는 이 시점으로 설정
         *  (8) level: 처음 시작 레벨 1로 기본 설정되어있음
         *  (9) purgeAt, deletedAt: 설정 안함
         *  (10) dailyPraise: AI 칭찬서 수신 여부. 기본값인 true로 설정되어있음
         *  (11) weeklyReportStyle: 주간 AI 상담서 스타일. 기본값인 SOFT로 설정되어있음
         *  (12) ink: 잉크 개수. 기본값인 0으로 생성됨
         */
        String accountCode = null;
        do{
            accountCode = AccountCodeGenerator.generateAccountCode();
        }
        while(userRepository.existsByAccountCode(accountCode));
        User user = User.builder()
                .accountCode(accountCode)
                .nickname(userNicknameGenerator.generateUniqueNickname())
                .lastLoginAt(LocalDateTime.now())
                .build();
        if(email != null){ user.updateEmail(email); }
        return userRepository.save(user); // AuthAccount -> User Entity의 연관관계 설정을 위해, UserRepository로 먼저 save한다.
    }

    /**
     * 사용자가 회원가입을 한 뒤, 기본적으로 사용자에게 할당해줘야할 것들을 할당해주는 함수.
     *  (1) 기본 UserItem 인스턴스 생성
     *  (2) 기본 Ability 인스턴스 생성
     *  (3) UserTerm 인스턴스 생성
     */
    private void allocateUser(User user){
        // 1. 사용자 UserItems 생성
        createDefaultUserItems(user);

        // 2. 사용자 Ability 생성
        createDefaultAbility(user);

        // 3. 사용자 약관 동의
        createDefaultUserTerms(user);

        // 4. Mission 생성
        createDefaultMission(user);
    }


    private List<UserItem> createDefaultUserItems(User user){
        /*
         * 1. Item들 중 name이 "Default.png"인 데이터들을 조회한다.
         * - 기본 아이템들을 못찾으면 예외처리
         */
        List<Item> defaultItems = itemRepository.findAllByName("Default.png");
        if(defaultItems.isEmpty()){throw new CustomException(ShopErrorCode.DEFAULT_ITEMS_NOT_FOUND);}

        defaultItems.forEach(defaultItem -> {log.info("{}", defaultItem.getFullUrl("example"));});


        // 2. 찾은 아이템들로 UserItem들을 생성해서 테이블에 저장
        List<UserItem> userItems = defaultItems.stream().map(item ->
                UserItem.builder()
                        .user(user)
                        .item(item)
                        .isEquipped(true)
                        .build()
        ).toList();
        return userItemRepository.saveAll(userItems);
    }

    /**
     * user 생성 시 ability 생성 로직 (능력치)
     * @param user 연동할 user
     */
    private Ability createDefaultAbility(User user) {
        Ability ability = Ability.builder()
                .user(user)
                .build();
        return abilityRepository.save(ability);
    }

    /**
     * user 생성 시 할당해줄
     */
    private List<UserTerm> createDefaultUserTerms(User user){
        // 1. 모든 약관들을 가져온다.
        List<Term> terms = termRepository.findAll();
        if(terms.isEmpty()){throw new CustomException(TermErrorCode.TERMS_NOT_FOUND);}

        // 2. 새로 만든 User와 가져온 Term들을 연결한다.
        List<UserTerm> userTerms = new ArrayList<>();
        for(Term term : terms){
            userTerms.add(
                    UserTerm.builder()
                            .term(term)
                            .user(user)
                            .build()
            );
        }
        return userTermRepository.saveAll(userTerms);
    }

    private Mission createDefaultMission(User user){
        Mission mission = Mission.builder()
                .user(user)
                .build();
        return missionRepository.save(mission);
    }

    @Transactional
    public UserNicknameResDto updateNickname(Long userId, UserNicknameUpdateReqDto reqDto) {
        // 1. User 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 닉네임 업데이트 (해당 닉네임의 스타일은 이미 Dto 레벨에서 검증되었음)
        user.updateNickname(reqDto.nickname());

        return UserNicknameResDto.builder().newNickname(user.getNickname()).build();
    }

    /**
     * 회원 탈퇴 로직을 수행하는 함수 (Soft Delete + 사용자 인증 데이터 삭제)
     * @param userId 탈퇴한 사용자 ID
     * @param accessToken 현재 요청에 사용된 Access Token
     */
    @Transactional
    public void withDrawAccount(Long userId, String accessToken){
        // 1. 사용자 인스턴스 가져오기 (비관적 락), 해당 사용자의 인증 정보 가져오기
        User user = userRepository.findByIdWithLock(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        AuthAccount userAuthAccount = authAccountRepository.findByUser(user).orElseThrow(() -> new CustomException(AuthErrorCode.USER_AUTH_ACCOUNT_NOT_FOUND));

        // 2. 이미 탈퇴 대기 중인 상태인지 확인(멱등성 보장)
        if(user.getStatus().equals(UserStatus.WITHDRAW_PENDING)){
            throw new CustomException(UserErrorCode.ALREADY_WITHDRAW_PENDING);
        }

        /*
         * 3. 해당 사용자 상태 최신화(비식별화)
         * (1) status -> WITHDRAW_PENDING
         * (2) deletedAt(삭제 요청 시각) 최신화
         * (3) purgeAt(완전 삭제 예정 시각) 최신화
         * (4) dailPraise (AI 칭찬서 수신 여부) false
         * (5) notificationEnabled (알림 설정) false
         */
        user.withdrawUser(purgeDurationInMs);

        // 4. 발급 받은 Access Token JTI Redis BlackList에 등록

        redisUtil.setTokenInBlacklist(resolveToken(accessToken));

        /*
         * 5. Redis에 저장된 Refresh Token 즉시 삭제 & 사용자와 연관된 RefreshTokenBackup 테이블 데이터 삭제
         * - 만약 refreshTokenBackup 테이블에 해당 내용이 없더라도, 회원탈퇴 로직을 계속해서 수행해야함
         * - 해당 사용자와 연관된 AuthAccount는 아직 삭제하지 않는다.(7일 뒤 스케줄러로 삭제한다)
         */
        List<RefreshTokenBackup> backups = refreshTokenBackupRepository.findAllByAuthAccount(userAuthAccount);

        // 5-1. Redis에서 각 토큰 삭제 (반복문)
        for (RefreshTokenBackup backup : backups) {
            redisUtil.deleteHashedRefreshToken(backup.getHashedTokenValue());
        }

        // 5-2. orphanRemoval 설정 활용하여 refreshTokenBackup 객체 삭제
        userAuthAccount.updateRefreshTokenBackup(null);

    }

    private String resolveToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    /** FCM 토큰 업데이트 */
    @Transactional
    public void updateFcmToken(Long userId, FcmTokenReqDto dto) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(UserErrorCode.USER_NOT_FOUND)
        );

        user.updateFcmToken(dto.fcmToken());
    }
}
