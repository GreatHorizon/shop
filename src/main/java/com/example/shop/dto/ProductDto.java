package com.example.shop.dto;

public record ProductDto(
        long id,
        String title,
        String description,
        String imgPath,
        int price,
        int count
) {

    public ProductDto withCount(Integer count) {
        return new ProductDto(id, title, description, imgPath, price, count);
    }
}
