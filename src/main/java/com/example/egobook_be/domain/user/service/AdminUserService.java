package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.exception.AdminUserErrorCode;
import com.example.egobook_be.domain.user.mapper.UserMapper;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final AbilityRepository abilityRepository;
    private final DiaryRepository diaryRepository;
    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserMapper userMapper;

    // 페이지 최대 크기 제한
    private static final int MAX_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_SIZE = 3;


    @Transactional(readOnly = true)
    public SliceResponse<SearchUserResDto> searchUserList(String keyword, UserStatus status, Integer page, Integer size) {
        /*
            1. Slicing을 위한 입력값 검증 & Pageable 객체 생성
            [검증]
                (1) keyword null or 빈칸 아닌지
                (2) Page 번호 1~n 범위인지
                (3) Page 크기 너무 크지는 않는지
            [동작]
                (1) Page 번호 값 -1
                (2) Page 크기 너무 크다면 크기 조정
                (3) Pageable 객체 생성 (우선 userId 기준으로 오름차순)
         */
        if(isKeywordNullOrBlank(keyword)){throw new CustomException(AdminUserErrorCode.KEYWORD_IS_NULL_OR_BLANK);}
        int validPage = getValidPage(page) - 1;
        int validSize = getValidPageSize(size);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.ASC, "id"));

        // 2. 해당 pageable 조건에 맞는 정보 Dto Projection으로 Slice 조회 및 반환
        Slice<SearchUserResDto> resDtos = userRepository.findUsersByKeywordAndStatus(keyword, status, pageable);
        log.info("관리자 회원 리스트 조회 성공 - keyword: {}, status: {}, page: {}, size: {}", keyword, status, validPage, validSize);
        return SliceResponse.of(resDtos);
    }

    /**
     * 관리자가 특정 사용자의 기본 정보를 조회하는 함수
     * @param userId : 조회할 사용자의 PK
     * @return AdminUserInfoResDto
     */
    @Transactional(readOnly = true)
    public AdminUserInfoResDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(AdminUserErrorCode.USER_NOT_FOUND));
        AuthAccount authAccount = authAccountRepository.findByUser(user).orElseThrow(() -> new CustomException(AdminUserErrorCode.AUTH_ACCOUNT_NOT_FOUND));
        log.info("관리자 회원 기본 정보 조회 성공 - userId: {}", userId);
        return userMapper.toAdminUserInfoResDto(user, authAccount);
    }

    /**
     * 특정 사용자의 활동 통계를 조회하는 함수
     * @param userId : 활동 통계를 조회할 사용자의 PK
     * @return AdminUserStatsResDto : 해당 사용자의 활동 통계 dto
     */
    @Transactional(readOnly = true)
    public AdminUserStatsResDto getUserStats(Long userId) {
        // 1. 해당 사용자, 사용자의 능력치 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AdminUserErrorCode.USER_NOT_FOUND));
        Ability ability = abilityRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(AdminUserErrorCode.ABILITY_NOT_FOUND));

        // 2. 해당 사용자의 각 활동(일기, 편지, 편지 답변, 오늘의 질문)의 횟수 조회
        long diaryCount = diaryRepository.countByUser(user);
        long letterCount = plazaLetterRepository.countBySenderId(userId);
        long letterReplyCount = plazaLetterReplyRepository.countByReplierId(userId);
        long questionAnswerCount = questionAnswerRepository.countByUser(user);

        log.info("관리자 회원 활동 통계 조회 성공 - userId: {}", userId);

        return userMapper.toAdminUserStatsResDto(user, ability, diaryCount, letterCount, letterReplyCount, questionAnswerCount);
    }

    private boolean isKeywordNullOrBlank(String keyword) {
        return keyword == null || keyword.trim().isEmpty();
    }

    private int getValidPage(Integer page){
        return (page != null && page >= 1) ? page : 1;
    }

    private int getValidPageSize(Integer size){
        int validSize = (size != null && size >= DEFAULT_PAGE_SIZE) ? size : DEFAULT_PAGE_SIZE;
        if (validSize > MAX_PAGE_SIZE) {
            validSize = MAX_PAGE_SIZE;
        }
        return validSize;
    }

}
