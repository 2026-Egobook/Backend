package com.example.egobook_be.domain.letters.mapper;

import com.example.egobook_be.domain.letters.dto.response.InboxNextResponse;
import com.example.egobook_be.domain.letters.dto.response.PlazaLetterDetailResDto;
import com.example.egobook_be.domain.letters.dto.response.PlazaReceivedReplyResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.dto.response.PlazaSentLetterResDto;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class PlazaLetterMapper {

    private static final String AI_PREVIEW = "48시간 동안 답장이 없어 내가 대신...";

    public PlazaSentLetterResDto toDto(PlazaLetter letter) {
        OffsetDateTime createdAt = letter.getCreatedAt();
        OffsetDateTime aiReplaceAt = (createdAt == null) ? null : createdAt.plusHours(48);

        String preview = (letter.getStatus() == PlazaLetterStatus.AI_REPLIED)
                ? AI_PREVIEW
                : truncate(letter.getContent(), 30);

        return PlazaSentLetterResDto.builder()
                .letterId(letter.getLetterId())
                .mode(letter.getMode())
                .status(letter.getStatus())
                .aiReplaceAt(aiReplaceAt)
                .lastMessagePreview(preview)
                .createdAt(createdAt)
                .backgroundColor(letter.getBackgroundColor().name())
                .build();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    public InboxNextResponse toResponse(PlazaLetter letter) {
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


    public PlazaReceivedReplyResDto toReceivedReplyDto(
            PlazaLetter letter,
            PlazaLetterReply reply,
            boolean reported
    ) {
        return PlazaReceivedReplyResDto.builder()
                .letterId(letter.getLetterId())
                .replyId(reply.getReplyId())
                .threadId(letter.getThreadId())

                .replyText(reply.getText())
                .repliedAt(reply.getCreatedAt())

                .aiGenerated(reply.isAiGenerated())
                .reported(reported)

                .mode(letter.getMode())
                .fromLabel(letter.getFromLabel())
                .backgroundColor(letter.getBackgroundColor().name())

                .build();
    }

    public PlazaLetterDetailResDto toDetailDto(
            PlazaLetter letter,
            PlazaLetterReply reply,
            boolean reported
    ) {
        PlazaLetterDetailResDto.ReplyDto replyDto = null;

        if (reply != null) {
            replyDto = PlazaLetterDetailResDto.ReplyDto.builder()
                    .replyId(reply.getReplyId())
                    .text(reply.getText())
                    .aiGenerated(reply.isAiGenerated())
                    .reported(reported)
                    .createdAt(reply.getCreatedAt())
                    .build();
        }

        return PlazaLetterDetailResDto.builder()
                .letterId(letter.getLetterId())
                .threadId(letter.getThreadId())

                .status(letter.getStatus())
                .mode(letter.getMode())

                .content(letter.getContent())
                .backgroundColor(letter.getBackgroundColor().name())

                .createdAt(letter.getCreatedAt())
                .arrivedAt(letter.getArrivedAt())

                .fromLabel(letter.getFromLabel())

                .reply(replyDto)
                .build();
    }

}
