package com.example.main.dto;

import java.util.List;

public record OrderDto(Long id, List<ProductDto> items, long totalSum) {
}

