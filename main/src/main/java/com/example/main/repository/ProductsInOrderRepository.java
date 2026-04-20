package com.example.main.repository;

import com.example.main.model.ProductsInOrderModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ProductsInOrderRepository extends R2dbcRepository<ProductsInOrderModel, Long> {
    Flux<ProductsInOrderModel> findByOrderId(Long orderId);
}
