package com.example.shop.dto;

import com.example.shop.model.CartActionModel;

public record ChangeCartStateItemRequest(Long id, CartActionModel action) {
}
