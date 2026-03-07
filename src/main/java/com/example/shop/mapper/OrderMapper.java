package com.example.shop.mapper;

import com.example.shop.dto.OrderDto;
import com.example.shop.model.OrderModel;

public class OrderMapper {
    public static OrderDto toDto(OrderModel model) {
        final var products = model
                .getProducts()
                .stream()
                .map(ProductMapper::toDto)
                .toList();

        return new OrderDto(model.getId(), products, model.getTotalSum());
    }
}
