package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.repository.AuthAccountRepository;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReportRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.repository.AnswerReportRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.user.dto.AdminUserInfoResDto;
import com.example.egobook_be.domain.user.dto.AdminUserReportHistoryResDto;
import com.example.egobook_be.domain.user.dto.AdminUserStatsResDto;
import com.example.egobook_be.domain.user.dto.SearchUserResDto;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.exception.AdminUserErrorCode;
import com.example.egobook_be.domain.user.mapper.AdminUserMapper;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.enums.ReportType;
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
    private final PlazaLetterReportRepository plazaLetterReportRepository;
    private final PlazaLetterReplyReportRepository plazaLetterReplyReportRepository;
    private final AnswerReportRepository questionAnswerReportRepository;
    private final AdminUserMapper adminUserMapper;
    private final RestrictionRepository restrictionRepository;

    // 페이지 최대 크기 제한
    private static final int MAX_PAGE_SIZE = 10;
    private static final int MIN_PAGE_SIZE = 1;


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
        return adminUserMapper.toAdminUserInfoResDto(user, authAccount);
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

        return adminUserMapper.toAdminUserStatsResDto(user, ability, diaryCount, letterCount, letterReplyCount, questionAnswerCount);
    }

    private boolean isKeywordNullOrBlank(String keyword) {
        return keyword == null || keyword.trim().isEmpty();
    }

    private int getValidPage(Integer page){
        return (page != null && page >= 1) ? page : 1;
    }

    private int getValidPageSize(Integer size){
        int validSize = (size != null && size >= MIN_PAGE_SIZE) ? size : MIN_PAGE_SIZE;
        if (validSize > MAX_PAGE_SIZE) {
            validSize = MAX_PAGE_SIZE;
        }
        return validSize;
    }

    /**
     * 해당 사용자가 신고받은/신고한 내역 리스트, 누적 신고 횟수, 과거 제재 이력등의 정보를 필터링해서 조회하는 API
     * @param userId : 조회할 일반 사용자의 PK
     * @param reportDomainType : 필터 옵션 - 신고 도메인 타입
     * @param reportType : 필터 옵션 - 신고 타입
     * @param reportReason : 필터 옵션 - 신고 이유
     * @param reportStatus : 필터 옵션 - 신고 처리 상태 필터
     * @param page : 페이지 번호
     * @param size : 페이지 사이즈
     * @return AdminUserReportHistoryResDto : 해당 사용자의 신고 기록 응답 DTO
     */
    @Transactional(readOnly = true)
    public AdminUserReportHistoryResDto getUserReportHistory(
            Long userId,
            ReportDomainType reportDomainType,
            ReportType reportType,
            ReportReason reportReason,
            ReportStatus reportStatus,
            Integer page,
            Integer size
    ) {
        log.info("관리자 회원 신고 이력 조회 시작 - userId: {} | [필터 설정] 신고 도메인: {}, 신고 타입: {}, 신고 사유: {}, 신고 상태: {}", userId, reportDomainType, reportType, reportReason, reportStatus);
        /*
            1. Slicing을 위한 입력값 검증 & Pageable 객체 생성
            [검증]
                (1) Page 번호 1~n 범위인지
                (2) Page 크기 너무 크지는 않는지
            [동작]
                (1) Page 번호 값 -1
                (2) Page 크기 너무 크다면 크기 조정
                (3) Pageable 객체 생성 (각 신고 데이터 createdAt 기준으로 내림차순)
         */
        int validPage = getValidPage(page) - 1;
        int validSize = getValidPageSize(size);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. 해당 사용자 있는지 검증
        if(!userRepository.existsById(userId)){
            throw new CustomException(AdminUserErrorCode.USER_NOT_FOUND);
        }

        // 3. 신고 도메인 타입별로 로직 분리 (LETTER | LETTER_REPLY | QUESTION_ANSWER)
        AdminUserReportHistoryResDto resDto = null;
        switch (reportDomainType) {
            case ReportDomainType.LETTER -> {
                resDto = getUserReportHistoryFromLetter(userId, reportType, reportReason, reportStatus, pageable);
            }
            case ReportDomainType.LETTER_REPLY -> {
                resDto = getUserReportHistoryFromLetterReply(userId, reportType, reportReason, reportStatus, pageable);
            }
            case ReportDomainType.QUESTION_ANSWER -> {
                resDto = getUserReportHistoryFromQuestionAnswer(userId, reportType, reportReason, reportStatus, pageable);
            }
        }

        // 4. 결과 확인 후 반환
        if(resDto == null){
            throw new CustomException(AdminUserErrorCode.GET_USER_REPORT_HISTORY_SERVER_EXCEPTION);
        }
        log.info("관리자 회원 신고 이력 조회 성공 - userId: {} | [필터 설정] 신고 도메인: {}, 신고 타입: {}, 신고 사유: {}, 신고 상태: {}", userId, reportDomainType, reportType, reportReason, reportStatus);
        return resDto;
    }

    /**
     * 편지에 대한 신고 정보 조회 함수
     * @param userId : 조회할 일반 사용자 PK
     * @param reportType : 필터 옵션 - 신고 타입
     * @param reportReason : 필터 옵션 - 신고 이유
     * @param reportStatus : 필터 옵션 - 신고 처리 상태 필터
     * @return AdminUserReportHistoryResDto : 해당 사용자의 신고 기록 응답 DTO
     */
    private AdminUserReportHistoryResDto getUserReportHistoryFromLetter(
            Long userId,
            ReportType reportType,
            ReportReason reportReason,
            ReportStatus reportStatus,
            Pageable pageable
    ){
        // 1. 필터링 조건 (ReportType, ReportReason, ReportStatus)에 맞는 Summary 객체 생성
        AdminUserReportHistoryResDto.Summary summary = getSummaryFromLetter(userId, reportType, reportReason, reportStatus);

        // 2. 필터링(ReportType, ReportReason, ReportStatus) & 검색 조건(Pageable) 에 맞는 데이터 검색
        SliceResponse<AdminUserReportHistoryResDto.ReportContent> reportList = getReportListFromLetter(userId, reportType, reportReason, reportStatus, pageable);

        // 3. Summary, ReportContent 이용해서 AdminUserReportHistoryResDto 생성 후 반환
        return adminUserMapper.toAdminUserReportHistoryResDto(summary, reportList);
    }

    // Letter에서 Summary 객체를 뽑아내는 함수
    private AdminUserReportHistoryResDto.Summary getSummaryFromLetter(Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus){
        long totalReportCount; // 1. 해당 사용자가 다른 사람이 작성한 편지를 신고한 누적 횟수
        long totalReportedCount; // 2. 해당 사용자가 작성한 편지가 신고받은 누적 횟수

        // 0. reportType이 null인 경우
        if (reportType == null) {
            // reportType 구분 없이 전체 카운트
            long total = plazaLetterReportRepository.countByUserId(userId, reportReason, reportStatus);
            totalReportCount = total;
            totalReportedCount = total;
        }
        // 1. reportType이 null이 아닌 경우
        else {
            // reportType 구분해서 전체 카운트
            totalReportCount = plazaLetterReportRepository.countByReporterId(userId, reportReason, reportStatus);
            totalReportedCount = plazaLetterReportRepository.countBySenderId(userId, reportReason, reportStatus);
        }

        // 2. 해당 사용자가 과거에 계정을 정지받은 횟수
        long pastSuspendedCount = restrictionRepository.countAllByUserId(userId);
        return AdminUserReportHistoryResDto.Summary.builder()
                .totalReportCount(totalReportCount)
                .totalReportedCount(totalReportedCount)
                .pastSuspendedCount(pastSuspendedCount)
                .build();
    }

    // Letter에서 필터링을 통해 Slice 객체로 매핑하는 함수
    private SliceResponse<AdminUserReportHistoryResDto.ReportContent> getReportListFromLetter(Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable){
        // 1. reportType이 null이면 전체 조회
        if (reportType == null) {
            Slice<PlazaLetterReport> slice = plazaLetterReportRepository
                    .findPlazaLetterReportsByUserIdWithoutReportType(userId, reportReason, reportStatus, pageable);
            return SliceResponse.of(slice, report ->
                    adminUserMapper.toReportContent(report, report.getLetter(), ReportDomainType.LETTER,
                            report.getReporterId().equals(userId) ? ReportType.REPORTER : ReportType.REPORTED));
        }
        // 2. reportType이 null이 아니면 각 reportType별로 결과 return
        return switch (reportType) {
            case ReportType.REPORTER -> {
                Slice<PlazaLetterReport> slice = plazaLetterReportRepository.findPlazaLetterReportsByReporterId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report -> adminUserMapper.toReportContent(report, report.getLetter(), ReportDomainType.LETTER, ReportType.REPORTER));
            }
            case ReportType.REPORTED -> {
                Slice<PlazaLetterReport> slice = plazaLetterReportRepository.findPlazaLetterReportsBySenderId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report -> adminUserMapper.toReportContent(report, report.getLetter(), ReportDomainType.LETTER, ReportType.REPORTED));
            }
        };
    }

    /**
     * 편지 답장에 대한 신고 정보 조회 함수
     * @param userId : 조회할 일반 사용자 PK
     * @param reportType : 필터 옵션 - 신고 타입
     * @param reportReason : 필터 옵션 - 신고 이유
     * @param reportStatus : 필터 옵션 - 신고 처리 상태 필터
     * @return AdminUserReportHistoryResDto : 해당 사용자의 신고 기록 응답 DTO
     */
    private AdminUserReportHistoryResDto getUserReportHistoryFromLetterReply(
            Long userId,
            ReportType reportType,
            ReportReason reportReason,
            ReportStatus reportStatus,
            Pageable pageable
    ) {
        // 1. 필터링 조건에 맞는 Summary 객체 생성
        AdminUserReportHistoryResDto.Summary summary =
                getSummaryFromLetterReply(userId, reportType, reportReason, reportStatus);

        // 2. 필터링 & 검색 조건에 맞는 데이터 검색
        SliceResponse<AdminUserReportHistoryResDto.ReportContent> reportList =
                getReportListFromLetterReply(userId, reportType, reportReason, reportStatus, pageable);

        // 3. AdminUserReportHistoryResDto 생성 후 반환
        return adminUserMapper.toAdminUserReportHistoryResDto(summary, reportList);
    }

    // LetterReply에서 Summary 객체를 뽑아내는 함수
    private AdminUserReportHistoryResDto.Summary getSummaryFromLetterReply(
            Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus
    ) {
        long totalReportCount;
        long totalReportedCount;

        if (reportType == null) {
            long total = plazaLetterReplyReportRepository.countByUserId(userId, reportReason, reportStatus);
            totalReportCount = total;
            totalReportedCount = total;
        } else {
            totalReportCount = plazaLetterReplyReportRepository.countByReporterId(userId, reportReason, reportStatus);
            totalReportedCount = plazaLetterReplyReportRepository.countByReplierId(userId, reportReason, reportStatus);
        }

        long pastSuspendedCount = restrictionRepository.countAllByUserId(userId);

        return AdminUserReportHistoryResDto.Summary.builder()
                .totalReportCount(totalReportCount)
                .totalReportedCount(totalReportedCount)
                .pastSuspendedCount(pastSuspendedCount)
                .build();
    }

    // LetterReply에서 필터링을 통해 Slice 객체로 매핑하는 함수
    private SliceResponse<AdminUserReportHistoryResDto.ReportContent> getReportListFromLetterReply(
            Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        if (reportType == null) {
            Slice<PlazaLetterReplyReport> slice = plazaLetterReplyReportRepository
                    .findPlazaLetterReplyReportsByUserIdWithoutReportType(userId, reportReason, reportStatus, pageable);
            return SliceResponse.of(slice, report ->
                    adminUserMapper.toReportContentFromLetterReply(report, report.getReply(), ReportDomainType.LETTER_REPLY,
                            report.getReporterId().equals(userId) ? ReportType.REPORTER : ReportType.REPORTED));
        }

        return switch (reportType) {
            case ReportType.REPORTER -> {
                Slice<PlazaLetterReplyReport> slice = plazaLetterReplyReportRepository
                        .findPlazaLetterReplyReportsByReporterId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report ->
                        adminUserMapper.toReportContentFromLetterReply(report, report.getReply(), ReportDomainType.LETTER_REPLY, ReportType.REPORTER));
            }
            case ReportType.REPORTED -> {
                Slice<PlazaLetterReplyReport> slice = plazaLetterReplyReportRepository
                        .findPlazaLetterReplyReportsByReplierId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report ->
                        adminUserMapper.toReportContentFromLetterReply(report, report.getReply(), ReportDomainType.LETTER_REPLY, ReportType.REPORTED));
            }
        };
    }

    /**
     * 오늘의 질문 답변에 대한 신고 정보 조회 함수
     * @param userId : 조회할 일반 사용자 PK
     * @param reportType : 필터 옵션 - 신고 타입
     * @param reportReason : 필터 옵션 - 신고 이유
     * @param reportStatus : 필터 옵션 - 신고 처리 상태 필터
     * @return AdminUserReportHistoryResDto : 해당 사용자의 신고 기록 응답 DTO
     */
    private AdminUserReportHistoryResDto getUserReportHistoryFromQuestionAnswer(
            Long userId,
            ReportType reportType,
            ReportReason reportReason,
            ReportStatus reportStatus,
            Pageable pageable
    ) {
        // 1. 필터링 조건에 맞는 Summary 객체 생성
        AdminUserReportHistoryResDto.Summary summary =
                getSummaryFromQuestionAnswer(userId, reportType, reportReason, reportStatus);

        // 2. 필터링 & 검색 조건에 맞는 데이터 검색
        SliceResponse<AdminUserReportHistoryResDto.ReportContent> reportList =
                getReportListFromQuestionAnswer(userId, reportType, reportReason, reportStatus, pageable);

        // 3. AdminUserReportHistoryResDto 생성 후 반환
        return adminUserMapper.toAdminUserReportHistoryResDto(summary, reportList);
    }

    // QuestionAnswer에서 Summary 객체를 뽑아내는 함수
    private AdminUserReportHistoryResDto.Summary getSummaryFromQuestionAnswer(
            Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus
    ) {
        long totalReportCount;
        long totalReportedCount;

        if (reportType == null) {
            long total = questionAnswerReportRepository.countByUserId(userId, reportReason, reportStatus);
            totalReportCount = total;
            totalReportedCount = total;
        } else {
            totalReportCount = questionAnswerReportRepository.countByReporterId(userId, reportReason, reportStatus);
            totalReportedCount = questionAnswerReportRepository.countByAnswererId(userId, reportReason, reportStatus);
        }

        long pastSuspendedCount = restrictionRepository.countAllByUserId(userId);

        return AdminUserReportHistoryResDto.Summary.builder()
                .totalReportCount(totalReportCount)
                .totalReportedCount(totalReportedCount)
                .pastSuspendedCount(pastSuspendedCount)
                .build();
    }

    // QuestionAnswer에서 필터링을 통해 Slice 객체로 매핑하는 함수
    private SliceResponse<AdminUserReportHistoryResDto.ReportContent> getReportListFromQuestionAnswer(
            Long userId, ReportType reportType, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        if (reportType == null) {
            Slice<AnswerReport> slice = questionAnswerReportRepository
                    .findAnswerReportsByUserIdWithoutReportType(userId, reportReason, reportStatus, pageable);
            return SliceResponse.of(slice, report ->
                    adminUserMapper.toReportContentFromQuestionAnswer(report, report.getAnswer(), ReportDomainType.QUESTION_ANSWER,
                            report.getUser().getId().equals(userId) ? ReportType.REPORTER : ReportType.REPORTED));
        }

        return switch (reportType) {
            case ReportType.REPORTER -> {
                Slice<AnswerReport> slice = questionAnswerReportRepository
                        .findAnswerReportsByReporterId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report ->
                        adminUserMapper.toReportContentFromQuestionAnswer(report, report.getAnswer(), ReportDomainType.QUESTION_ANSWER, ReportType.REPORTER));
            }
            case ReportType.REPORTED -> {
                Slice<AnswerReport> slice = questionAnswerReportRepository
                        .findAnswerReportsByAnswererId(userId, reportReason, reportStatus, pageable);
                yield SliceResponse.of(slice, report ->
                        adminUserMapper.toReportContentFromQuestionAnswer(report, report.getAnswer(), ReportDomainType.QUESTION_ANSWER, ReportType.REPORTED));
            }
        };
    }
}
