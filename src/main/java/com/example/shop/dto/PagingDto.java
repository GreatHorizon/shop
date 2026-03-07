package com.example.shop.dto;

public record PagingDto(int pageSize, int pageNumber, Boolean hasPrevious, Boolean hasNext) {
}
