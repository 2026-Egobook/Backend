package com.example.egobook_be.domain.letters.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WordDetectResponse {

    private String text;

    private double percentage;

    @JsonProperty("is_harmful")
    private boolean harmful;

    private String label;

    @JsonProperty("bad_words")
    private List<String> badWords;
}
