package com.example.egobook_be.domain.stat.enums;

import lombok.Getter;

@Getter
public enum AdminStatUnit {
    WEEK("%x-W%v"),
    MONTH("%Y-%m");

    private final String format;

    AdminStatUnit(String format) {
        this.format = format;
    }
}