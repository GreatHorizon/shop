package com.example.shop.repository;


import com.example.shop.model.OrderModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<OrderModel, Integer> {
    Mono<OrderModel> getOrderById(long id);
}