package com.example.egobook_be.domain.letters.enums;

public enum PlazaLetterColor {
    WHITE(0),  // 기본 하얀색, 가격 0원
    PINK(150),  // 핑크색, 가격 150원
    GREEN(200),  // 초록색, 가격 200원
    BLUE(300),  // 파란색, 가격 300원
    PURPLE(400);  // 보라색, 가격 400원

    private final int price;

    PlazaLetterColor(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

}

