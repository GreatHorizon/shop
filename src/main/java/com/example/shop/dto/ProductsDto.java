package com.example.shop.dto;

import java.util.List;

public record ProductsDto(List<List<ProductDto>> items, PagingDto pagingDto) {

}
