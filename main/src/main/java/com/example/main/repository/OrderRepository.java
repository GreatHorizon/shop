package com.example.main.repository;


import com.example.main.model.OrderModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<OrderModel, Long> {
    Mono<OrderModel> getOrderById(long id);
}