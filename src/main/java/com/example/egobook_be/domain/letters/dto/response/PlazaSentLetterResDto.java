package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.PlazaLetterMode;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlazaSentLetterResDto {

    private Long letterId;
    private PlazaLetterMode mode;
    private PlazaLetterStatus status;

    // createdAt + 48h (AI 대체 예정/발생 시간)
    private LocalDateTime aiReplaceAt;

    // 리스트에서 보여줄 미리보기 텍스트
    private String lastMessagePreview;

    private LocalDateTime createdAt;
    private String backgroundColor;
}
