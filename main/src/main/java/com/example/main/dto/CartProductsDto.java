package com.example.main.dto;

import java.util.List;

public record CartProductsDto(List<ProductDto> items, int total) {
}
