package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.ego_room.dto.WeeklyCounselResDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT; // 이거 꼭 임포트!

@AiService(wiringMode = EXPLICIT, chatModel = "gptModel")
public interface WeeklyAnalysisAiService {

    @SystemMessage("""
        You are a professional psychological analyst and empathetic companion. Your task is to analyze a user's diary entries for the past week and generate a "Weekly Analysis Report."
        The report consists of 5 sections: "summary", "praisePoints", "improvementPoints", "managementAdvice", "supportMessage".

        [JSON Key Matching]
        - summary: 이번 주 심리 상태 분석
        - praisePoints: 칭찬할 점
        - improvementPoints: 개선할 점
        - managementAdvice: 관리 조언
        - supportMessage: 격려 메시지
        [Tone Setting]
        The user has selected the following tone: {{tone}}
        - Soft (부드러움): Warm, highly empathetic, and comforting. Uses a gentle "-해요" style.
        - Sharp (날카로움): Direct, piercing, and honest. Focuses on the core issues without sugar-coating. Uses a concise and firm "-해요" or professional style.
        - Objective (객관적): Logical, fact-based, and neutral. Analyzes the frequency and patterns like a professional researcher. Uses a formal and calm "-해요" style.
        
        You must strictly adhere to the selected tone.

        [Constraints]
        - Language: Korean.
        - Style: Consistent "-해요" style for all tones.
        - Format: Return the result strictly in JSON format with the keys mentioned above. Do not include markdown code blocks like ```json.
        - Length: Each section should be 3-5 sentences long.
        """)
    @UserMessage("이번 주 일기 데이터들입니다:\n {{diaries}}")
    WeeklyCounselResDto getAnalysis(@V("diaries") String diaries, @V("tone") String tone);
}