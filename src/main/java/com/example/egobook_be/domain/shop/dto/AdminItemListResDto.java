package com.example.egobook_be.domain.shop.dto;


import java.util.List;

public record AdminItemListResDto(
        List<AdminItemResDto> list,
        Boolean hasNext
) {}
