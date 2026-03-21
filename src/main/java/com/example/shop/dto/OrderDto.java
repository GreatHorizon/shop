package com.example.shop.dto;

import java.util.List;

public record OrderDto(Long id, List<ProductDto> items, long totalSum) {
}

