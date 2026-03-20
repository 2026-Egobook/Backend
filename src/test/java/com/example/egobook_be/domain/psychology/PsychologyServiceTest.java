package com.example.egobook_be.domain.psychology;

import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import com.example.egobook_be.domain.psychology.entity.UserKnowledge;
import com.example.egobook_be.domain.psychology.repository.PsychologyKnowledgeRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.psychology.service.PsychologyService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PsychologyServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PsychologyKnowledgeRepository psychologyKnowledgeRepository;
    @Mock private UserKnowledgeRepository userKnowledgeRepository;

    @InjectMocks
    private PsychologyService psychologyService;

    @Test
    @DisplayName("saveKnowledge_새로운북마크저장_성공")
    void saveKnowledge_newBookmark_success() {
        // given
        Long userId = 1L;
        Long knowledgeId = 10L;

        User user = User.builder().id(userId).build();

        PsychologyKnowledge knowledge = mock(PsychologyKnowledge.class);
        when(knowledge.getId()).thenReturn(knowledgeId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(psychologyKnowledgeRepository.findById(knowledgeId)).thenReturn(Optional.of(knowledge));
        when(userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)).thenReturn(Optional.empty());

        // when
        psychologyService.saveKnowledge(userId, knowledgeId);

        // then
        verify(userKnowledgeRepository, times(1)).save(any(UserKnowledge.class));
    }

    @Test
    @DisplayName("saveKnowledge_이미북마크됨_실패")
    void saveKnowledge_alreadyBookmarked_fail() {
        // given
        Long userId = 1L;
        Long knowledgeId = 10L;

        User user = User.builder().id(userId).build();
        PsychologyKnowledge knowledge = mock(PsychologyKnowledge.class);
        when(knowledge.getId()).thenReturn(knowledgeId);

        UserKnowledge uk = mock(UserKnowledge.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(psychologyKnowledgeRepository.findById(knowledgeId)).thenReturn(Optional.of(knowledge));
        when(userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)).thenReturn(Optional.of(uk));

        // 이미 북마크 되어 있고 삭제되지 않은 상태 설정
        when(uk.isBookmarked()).thenReturn(true);
        when(uk.getDeletedAt()).thenReturn(null);

        // when & then
        assertThrows(CustomException.class, () -> {
            psychologyService.saveKnowledge(userId, knowledgeId);
        });
    }

    @Test
    @DisplayName("deleteSavedKnowledge_북마크취소_성공")
    void deleteSavedKnowledge_success() {
        // given
        Long userId = 1L;
        Long knowledgeId = 10L;

        User user = User.builder().id(userId).build();
        PsychologyKnowledge knowledge = mock(PsychologyKnowledge.class);
        when(knowledge.getId()).thenReturn(knowledgeId);

        UserKnowledge uk = mock(UserKnowledge.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(psychologyKnowledgeRepository.findById(knowledgeId)).thenReturn(Optional.of(knowledge));
        when(userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)).thenReturn(Optional.of(uk));

        when(uk.isBookmarked()).thenReturn(true);

        // when
        psychologyService.deleteSavedKnowledge(userId, knowledgeId);

        // then
        verify(uk, times(1)).delete();
    }

    @Test
    @DisplayName("deleteSavedKnowledge_북마크안된지식_실패")
    void deleteSavedKnowledge_notBookmarked_fail() {
        // given
        Long userId = 1L;
        Long knowledgeId = 10L;

        User user = User.builder().id(userId).build();
        PsychologyKnowledge knowledge = mock(PsychologyKnowledge.class);
        when(knowledge.getId()).thenReturn(knowledgeId);

        UserKnowledge uk = mock(UserKnowledge.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(psychologyKnowledgeRepository.findById(knowledgeId)).thenReturn(Optional.of(knowledge));
        when(userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)).thenReturn(Optional.of(uk));

        // 북마크가 안 된 상태 설정
        when(uk.isBookmarked()).thenReturn(false);

        // when & then
        assertThrows(CustomException.class, () -> {
            psychologyService.deleteSavedKnowledge(userId, knowledgeId);
        });
    }
}