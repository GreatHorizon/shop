package com.example.main.dto;

import com.example.main.model.CartActionModel;
import com.example.main.model.SortModel;

public record ChangeCartStateItemsRequest(Long id, CartActionModel action, String search, SortModel sort, Integer pageSize, Integer pageNumber) {
}