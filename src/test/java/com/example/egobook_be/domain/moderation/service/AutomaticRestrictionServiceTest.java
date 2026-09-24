package com.example.egobook_be.domain.moderation.service;

import com.example.egobook_be.domain.moderation.entity.ReportStrike;
import com.example.egobook_be.domain.moderation.repository.ModerationUserLockRepository;
import com.example.egobook_be.domain.moderation.repository.ReportStrikeRepository;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomaticRestrictionServiceTest {
    @Mock ReportStrikeRepository strikes;
    @Mock ModerationUserLockRepository users;
    @Mock RestrictionRepository restrictions;
    @Mock ModerationContentCleanupService cleanup;
    AutomaticRestrictionService service;

    @BeforeEach void setup() {
        service = new AutomaticRestrictionService(strikes, users, restrictions, cleanup);
    }

    @Test void twoLetterAndReplyReportsDoNotRestrict() {
        when(strikes.countByTargetUserIdAndSourceTypeIn(eq(10L), anyCollection())).thenReturn(2L);
        service.onReportSaved(ReportStrike.SourceType.REPLY, 20L, 10L, null);
        verify(strikes).saveAndFlush(any(ReportStrike.class));
        verifyNoInteractions(restrictions, cleanup);
    }

    @Test void thirdLetterOrReplyReportRestrictsBothDomainsAndCleansUp() {
        when(strikes.countByTargetUserIdAndSourceTypeIn(eq(10L), anyCollection())).thenReturn(3L);
        service.onReportSaved(ReportStrike.SourceType.LETTER, 21L, 10L, null);
        verify(restrictions, times(2)).save(any(Restriction.class));
        verify(cleanup).archiveAndDeleteAllAuthoredContent(10L);
    }

    @Test void thirdAnswerReportRestrictsBothDomainsAndCleansUp() {
        when(strikes.countBySourceTypeAndAnswerId(ReportStrike.SourceType.ANSWER, 55L)).thenReturn(3L);
        service.onReportSaved(ReportStrike.SourceType.ANSWER, 33L, 10L, 55L);
        verify(restrictions, times(2)).save(any(Restriction.class));
        verify(cleanup).archiveAndDeleteAllAuthoredContent(10L);
    }

    @Test void fourthReportDoesNotReTriggerSameLifetimeThreshold() {
        when(strikes.countByTargetUserIdAndSourceTypeIn(eq(10L), anyCollection())).thenReturn(4L);
        service.onReportSaved(ReportStrike.SourceType.REPLY, 44L, 10L, null);
        verifyNoInteractions(restrictions, cleanup);
    }
}
