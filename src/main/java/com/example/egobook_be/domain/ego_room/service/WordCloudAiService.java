package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.ego_room.dto.WordCloudDto;
import com.example.egobook_be.domain.ego_room.dto.WordCloudResDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import java.util.List;
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "gptModel")
public interface WordCloudAiService {

    @SystemMessage("""
        You are an expert in Korean Natural Language Processing (NLP). 
        Analyze the provided diary entries and extract the top 20 most meaningful NOUNS.
        
        [Strict Rules]
        1. Extract only high-value NOUNS that represent the user's activities, emotions, or key events.
        2. EXCLUDE meaningless adverbs, pronouns, or common fillers (e.g., '정말', '매우', '오늘', '내일', '생각', '진심으로', '것입니다', '합니다').
        3. IGNORE repetitive template phrases like "내일은 오늘보다 더 성장한 내가 되기를...".
        4. Assign a 'weight' (1-100) to each noun based on its frequency and emotional significance.
        5. Your response MUST be a pure JSON array of objects, with no extra text or markdown formatting.
        
        [Format Example]
        [{"word": "여행", "weight": 95}, {"word": "행복", "weight": 80}]
        """)
    WordCloudResDto extractKeywords(@UserMessage String diaryContent);
}