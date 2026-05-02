package com.example.egobook_be.domain.psychology.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeListResDto;
import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeReqDto;
import com.example.egobook_be.domain.psychology.dto.AdminKnowledgeResDto;
import com.example.egobook_be.domain.psychology.dto.KnowledgeInfoResDto;
import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import com.example.egobook_be.domain.psychology.exception.PsychologyErrorCode;
import com.example.egobook_be.domain.psychology.repository.PsychologyKnowledgeRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPsychologyService {

    private final PsychologyKnowledgeRepository psychologyKnowledgeRepository;
    @Transactional
    public KnowledgeInfoResDto createKnowledge(AdminKnowledgeReqDto reqDto) {
        PsychologyKnowledge knowledge = PsychologyKnowledge.builder()
                .content(reqDto.content())
                .source(reqDto.source())
                .build();

        PsychologyKnowledge savedKnowledge = psychologyKnowledgeRepository.save(knowledge);

        return new KnowledgeInfoResDto(
                savedKnowledge.getId(),
                "저장된 심리 지식",
                savedKnowledge.getContent(),
                savedKnowledge.getSource()
        );

    }
    @Transactional(readOnly = true)
    public AdminKnowledgeListResDto getAllKnowledgeList(int page, int size) {
        // 사용자가 입력한 1페이지를 0페이지로 변환하여 처리
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        Slice<PsychologyKnowledge> sliceResult = psychologyKnowledgeRepository.findAllByDeletedAtIsNull(pageable);

        List<AdminKnowledgeListResDto.AdminKnowledgeItemResDto> items = sliceResult.getContent().stream()
                .map(k -> new AdminKnowledgeListResDto.AdminKnowledgeItemResDto(
                        k.getId(),
                        k.getContent(),
                        k.getSource(),
                        k.getCreatedAt(),
                        k.getDeletedAt()
                ))
                .toList();

        return new AdminKnowledgeListResDto(items, sliceResult.hasNext());
    }

    @Transactional(readOnly = true)
    public AdminKnowledgeResDto getKnowledge(Long id) {
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(id)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.KNOWLEDGE_NOT_FOUND));
        return new AdminKnowledgeResDto(knowledge.getId(), knowledge.getContent(), knowledge.getSource(), knowledge.getCreatedAt(),knowledge.getDeletedAt());
    }

    @Transactional
    public AdminKnowledgeResDto updateKnowledge(Long id, AdminKnowledgeReqDto reqDto) {
        PsychologyKnowledge oldKnowledge = psychologyKnowledgeRepository.findById(id)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.KNOWLEDGE_NOT_FOUND));

        // 기존 지식을 소프트 딜리트 처리
        oldKnowledge.delete();

        // 새로운 지식 엔티티 생성 및 저장
        PsychologyKnowledge newKnowledge = PsychologyKnowledge.builder()
                .content(reqDto.content())
                .source(reqDto.source())
                .build();

        PsychologyKnowledge saved = psychologyKnowledgeRepository.save(newKnowledge);

        return new AdminKnowledgeResDto(
                saved.getId(),
                saved.getContent(),
                saved.getSource(),
                saved.getCreatedAt(),
                saved.getDeletedAt()
        );
         }

    @Transactional
    public void deleteKnowledge(Long id) {
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(id)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.KNOWLEDGE_NOT_FOUND));

        knowledge.delete();
    }
}
