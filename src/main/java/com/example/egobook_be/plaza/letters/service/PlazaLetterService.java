package com.example.egobook_be.plaza.letters.service;

import com.example.egobook_be.plaza.letters.domain.PlazaLetter;
import com.example.egobook_be.plaza.letters.domain.PlazaLetterStatus;
import com.example.egobook_be.plaza.letters.dto.InboxNextResponse;
import com.example.egobook_be.plaza.letters.repository.PlazaLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlazaLetterService {

    private final PlazaLetterRepository plazaLetterRepository;

    @Transactional(readOnly = true)
    public InboxNextResponse getNextArrivedLetter(Long userId) {
        return plazaLetterRepository
                .findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(userId, PlazaLetterStatus.ARRIVED)
                .map(this::toResponse)
                .orElseGet(InboxNextResponse::empty);
    }

    private InboxNextResponse toResponse(PlazaLetter letter) {
        return InboxNextResponse.builder()
                .letter(InboxNextResponse.LetterDto.builder()
                        .letterId(letter.getLetterId())
                        .status(letter.getStatus())
                        .mode(letter.getMode())
                        .fromLabel(letter.getFromLabel())
                        .content(letter.getContent())
                        .arrivedAt(letter.getArrivedAt())
                        .replyDeadlineAt(letter.getReplyDeadlineAt())
                        .build())
                .build();
    }
}
