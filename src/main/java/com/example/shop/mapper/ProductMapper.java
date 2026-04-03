package com.example.shop.mapper;

import com.example.shop.dto.ProductDto;
import com.example.shop.model.ProductModel;

public class ProductMapper {
    public static ProductDto toDto(ProductModel model) {
        return new ProductDto(
                model.id(),
                model.title(),
                model.description(),
                model.mainImagePath(),
                model.price(),
                0
        );
    }

    public static ProductDto toDto(ProductModel model, int count) {
        return new ProductDto(
                model.id(),
                model.title(),
                model.description(),
                model.mainImagePath(),
                model.price(),
                count
        );
    }
}
