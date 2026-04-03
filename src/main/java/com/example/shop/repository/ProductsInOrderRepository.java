package com.example.shop.repository;

import com.example.shop.model.ProductsInOrderModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ProductsInOrderRepository extends R2dbcRepository<ProductsInOrderModel, Long> {
    Flux<ProductsInOrderModel> findByOrderId(Long orderId);
}
