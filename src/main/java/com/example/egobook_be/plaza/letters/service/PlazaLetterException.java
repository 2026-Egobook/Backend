package com.example.egobook_be.plaza.letters.service;

import lombok.Getter;

@Getter
public class PlazaLetterException extends RuntimeException {
    private final String code;

    public PlazaLetterException(String code, String message) {
        super(message);
        this.code = code;
    }
}
