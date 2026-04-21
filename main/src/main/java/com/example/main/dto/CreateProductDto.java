package com.example.main.dto;

public record CreateProductDto(String title,
                               String description,
                               String mainImagePath,
                               int price) {

    public CreateProductDto withImage(String mainImagePath) {
        return new CreateProductDto(title, description, mainImagePath, price);
    }

    public CreateProductDto() {
        this(null, null, null, 0);
    }
}
