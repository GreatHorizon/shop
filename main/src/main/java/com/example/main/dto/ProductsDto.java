package com.example.main.dto;

import java.util.List;

public record ProductsDto(List<List<ProductDto>> items, PagingDto pagingDto) {

}
