package com.example.main.dto;

import com.example.main.model.CartActionModel;

public record ChangeCartStateItemRequest(Long id, CartActionModel action) {
}
