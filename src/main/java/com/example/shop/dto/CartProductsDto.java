package com.example.shop.dto;

import java.util.List;

public record CartProductsDto(List<ProductDto> items, int total) {
}
