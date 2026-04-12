package com.example.main.mapper;

import com.example.main.dto.ProductDto;
import com.example.main.model.ProductModel;

public class ProductMapper {
    public static ProductDto toDto(ProductModel model) {
        return new ProductDto(
                model.getId(),
                model.getTitle(),
                model.getDescription(),
                model.getMainImagePath(),
                model.getPrice(),
                0
        );
    }

    public static ProductDto toDto(ProductModel model, int count) {
        return new ProductDto(
                model.getId(),
                model.getTitle(),
                model.getDescription(),
                model.getMainImagePath(),
                model.getPrice(),
                count
        );
    }
}
