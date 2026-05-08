package com.example.egobook_be.domain.letters.enums;

public enum PlazaLetterColor {
    WHITE(0),  // 기본 하얀색, 가격 0원
    PINK(75),  // 핑크색, 가격 75원
    GREEN(100),  // 초록색, 가격 100원
    BLUE(150),  // 파란색, 가격 150원
    PURPLE(175);  // 보라색, 가격 175원

    private final int price;

    PlazaLetterColor(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

}

