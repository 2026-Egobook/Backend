package com.example.egobook_be.domain.letters.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlazaLetterColor {
    WHITE(0, "White.png"),  // 기본 하얀색, 가격 0원
    PINK(75, "Pink.png"),  // 핑크색, 가격 75원
    GREEN(100, "Green.png"),  // 초록색, 가격 100원
    BLUE(150, "Blue.png"),  // 파란색, 가격 150원
    PURPLE(175, "Purple.png");  // 보라색, 가격 175원

    private final int price;
    private final String imageName;

}

