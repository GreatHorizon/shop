package com.example.shop.dto;

import com.example.shop.model.CartActionModel;
import com.example.shop.model.SortModel;

public record ChangeCartStateItemsRequest(Long id, CartActionModel action, String search, SortModel sort, Integer pageSize, Integer pageNumber) {
}