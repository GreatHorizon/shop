package com.example.main.dto;

public record PagingDto(int pageSize, int pageNumber, Boolean hasPrevious, Boolean hasNext) {
}
