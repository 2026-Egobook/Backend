package com.example.egobook_be.domain.moderation.service;

import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.repository.*;
import com.example.egobook_be.domain.moderation.entity.ModerationReportArchive;
import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.repository.ModerationReportArchiveRepository;
import com.example.egobook_be.domain.question.entity.*;
import com.example.egobook_be.domain.question.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModerationContentCleanupService {
    private final PlazaLetterRepository letters;
    private final PlazaLetterReplyRepository replies;
    private final PlazaLetterReportRepository letterReports;
    private final PlazaLetterReplyReportRepository replyReports;
    private final QuestionAnswerRepository answers;
    private final AnswerReportRepository answerReports;
    private final ModerationReportArchiveRepository archives;

    @Transactional(propagation = Propagation.MANDATORY)
    public void archiveAndDeleteAllAuthoredContent(Long authorId) {
        List<PlazaLetter> authoredLetters = letters.findAllBySenderId(authorId);
        List<Long> letterIds = authoredLetters.stream().map(PlazaLetter::getLetterId).toList();
        // Deleting authored letters also deletes replies on those letters (FK dependency).
        Map<Long, PlazaLetterReply> allReplies = replies.findAllByReplierId(authorId).stream()
                .collect(Collectors.toMap(PlazaLetterReply::getReplyId, Function.identity()));
        if (!letterIds.isEmpty()) {
            for (PlazaLetterReply reply : replies.findAllByLetter_LetterIdIn(letterIds)) {
                allReplies.put(reply.getReplyId(), reply);
            }
        }
        List<Long> replyIds = new ArrayList<>(allReplies.keySet());
        // Archive ALL report details before removing FK-dependent rows.
        if (!replyIds.isEmpty()) {
            for (PlazaLetterReplyReport r : replyReports.findAllByReply_ReplyIdIn(replyIds)) {
                PlazaLetterReply reply = r.getReply();
                archives.save(ModerationReportArchive.of(ReportStrike.SourceType.REPLY,
                        r.getReportId(), r.getReplierId(), reply.getReplyId(), r.getReporterId(),
                        r.getReason() == null ? null : r.getReason().name(), r.getDescription(),
                        r.getStatus().name(), r.getAdminMemo(), r.getCreatedAt(), reply.getContent()));
            }
        }
        if (!letterIds.isEmpty()) {
            for (PlazaLetterReport r : letterReports.findAllByLetter_LetterIdIn(letterIds)) {
                archives.save(ModerationReportArchive.of(ReportStrike.SourceType.LETTER,
                        r.getReportId(), r.getSenderId(), r.getLetter().getLetterId(), r.getReporterId(),
                        r.getReason() == null ? null : r.getReason().name(), r.getDescription(),
                        r.getStatus().name(), r.getAdminMemo(), r.getCreatedAt(), r.getLetter().getContent()));
            }
        }
        // Responses authored by this user are removed with reports archived first.
        List<QuestionAnswer> authoredAnswers = answers.findAllByUser_Id(authorId);
        List<Long> answerIds = authoredAnswers.stream().map(QuestionAnswer::getId).toList();
        if (!answerIds.isEmpty()) {
            for (AnswerReport r : answerReports.findAllByAnswer_IdIn(answerIds)) {
                archives.save(ModerationReportArchive.of(ReportStrike.SourceType.ANSWER,
                        r.getId(), r.getAnswer().getUser().getId(), r.getAnswer().getId(), r.getUser().getId(),
                        r.getReason() == null ? null : r.getReason().name(), r.getDescription(),
                        r.getStatus().name(), r.getAdminMemo(), r.getCreatedAt(), r.getAnswer().getContent()));
            }
        }
        archives.flush();
        if (!replyIds.isEmpty()) {
            replyReports.deleteAllByReplyIdIn(replyIds);
            replies.deleteAllByReplyIdIn(replyIds);
        }
        if (!letterIds.isEmpty()) {
            letterReports.deleteAllByLetterIdIn(letterIds);
            letters.deleteAllByLetterIdIn(letterIds);
        }
        if (!answerIds.isEmpty()) {
            answerReports.deleteAllByAnswerIdIn(answerIds);
            answers.deleteAllByIdInBatch(answerIds);
        }
    }
}
