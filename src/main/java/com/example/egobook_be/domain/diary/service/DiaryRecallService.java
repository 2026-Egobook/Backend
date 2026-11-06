package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.dto.DiaryRecallResDto;
import com.example.egobook_be.domain.diary.dto.DiaryRecallSeenResDto;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.mapper.DiaryMapper;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryRecallService {

    /** 노출 주기 (일) */
    static final int RECALL_CYCLE_DAYS = 21;

    private final DiaryRepository diaryRepository;
    private final DiaryQueryService diaryQueryService;

    /**
     * 과거의 나 돌아보기 팝업에 노출할 일기 조회
     * - 마지막 노출일(lastRecalledAt)이 없거나 21일이 지났을 때만 노출 대상이 된다.
     * - 1순위: 오늘 기준 21일 전 날짜의 일기 / 2순위: 오늘 이전 가장 오래된 날짜의 일기
     * - 같은 날짜에 일기가 여러 개면 (userId + 오늘 날짜) 시드로 1개를 고른다. 같은 날 재호출해도 같은 일기가 반환된다.
     * - 상태를 변경하지 않는다. 노출 확정은 markRecallSeen()에서 처리한다.
     * @param userId : 사용자 ID
     * @return : 노출 여부와 일기 정보 (미노출 시 available=false, diary=null)
     */
    @Transactional(readOnly = true)
    public DiaryRecallResDto getRecallDiary(Long userId) {
        log.info("[DiaryRecallService] getRecallDiary() - START | userId: {}", userId);

        User user = diaryQueryService.getUserById(userId);
        LocalDate today = LocalDate.now();

        if (!isRecallDue(user.getLastRecalledAt(), today)) {
            log.info("[DiaryRecallService] getRecallDiary() - END | userId: {}, available: false (cycle)", userId);
            return DiaryMapper.toDiaryRecallDto(null);
        }

        Diary diary = selectRecallDiary(userId, today);

        log.info("[DiaryRecallService] getRecallDiary() - END | userId: {}, diaryId: {}",
                userId, diary == null ? null : diary.getId());
        return DiaryMapper.toDiaryRecallDto(diary);
    }

    /**
     * 과거의 나 돌아보기 팝업 노출 확정
     * - 클라이언트가 팝업을 실제로 띄운 뒤 호출한다.
     * - lastRecalledAt을 오늘로 저장해 다음 21일 주기를 시작한다.
     * - 같은 날 재호출은 허용한다(멱등). 주기가 돌아오지 않았으면 RECALL_NOT_AVAILABLE 예외.
     * @param userId  : 사용자 ID
     * @param diaryId : 노출된 일기 ID (본인 일기인지 검증)
     * @return : 노출 확정일과 다음 노출 가능일
     */
    @Transactional
    public DiaryRecallSeenResDto markRecallSeen(Long userId, Long diaryId) {
        log.info("[DiaryRecallService] markRecallSeen() - START | userId: {}, diaryId: {}", userId, diaryId);

        diaryQueryService.getDiaryWithAuth(userId, diaryId);
        User user = diaryQueryService.getUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDate lastRecalledAt = user.getLastRecalledAt();

        if (!today.equals(lastRecalledAt) && !isRecallDue(lastRecalledAt, today)) {
            throw new CustomException(DiaryErrorCode.RECALL_NOT_AVAILABLE);
        }

        user.updateLastRecalledAt(today);
        DiaryRecallSeenResDto result = DiaryMapper.toDiaryRecallSeenDto(today, today.plusDays(RECALL_CYCLE_DAYS));

        log.info("[DiaryRecallService] markRecallSeen() - END | userId: {}, nextRecallDate: {}", userId, result.nextRecallDate());
        return result;
    }

    /**
     * 노출 주기 도래 여부 판단
     * @param lastRecalledAt : 마지막 노출일 (null이면 첫 노출)
     * @param today          : 오늘 날짜
     * @return : 주기 도래 시 true
     */
    private boolean isRecallDue(LocalDate lastRecalledAt, LocalDate today) {
        return lastRecalledAt == null || !today.isBefore(lastRecalledAt.plusDays(RECALL_CYCLE_DAYS));
    }

    /**
     * 노출할 일기 선정
     * - 1순위: 21일 전 날짜 / 2순위: 오늘 이전 가장 오래된 날짜
     * - 삭제는 hard delete라 별도 필터가 필요 없다.
     * @param userId : 사용자 ID
     * @param today  : 오늘 날짜
     * @return : 선정된 일기, 후보가 없으면 null
     */
    private Diary selectRecallDiary(Long userId, LocalDate today) {
        List<Long> candidateIds = diaryRepository.findIdsByUserIdAndDate(userId, today.minusDays(RECALL_CYCLE_DAYS));

        if (candidateIds.isEmpty()) {
            LocalDate oldestDate = diaryRepository.findOldestDateBefore(userId, today);
            if (oldestDate == null) {
                return null;
            }
            candidateIds = diaryRepository.findIdsByUserIdAndDate(userId, oldestDate);
        }

        if (candidateIds.isEmpty()) {
            return null;
        }

        int index = Math.floorMod(Objects.hash(userId, today), candidateIds.size());
        return diaryRepository.findById(candidateIds.get(index)).orElse(null);
    }
}