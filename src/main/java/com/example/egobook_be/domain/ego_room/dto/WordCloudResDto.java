package com.example.egobook_be.domain.ego_room.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class WordCloudResDto {
    private List<WordCloudDto> keywords;
}