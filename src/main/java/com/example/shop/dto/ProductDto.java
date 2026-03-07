package com.example.shop.dto;

public record ProductDto(
        long id,
        String title,
        String description,
        String imgPath,
        int price,
        int count
) {
}
