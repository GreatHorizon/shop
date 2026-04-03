package com.example.shop.repository;

import com.example.shop.model.ProductsInCartModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends R2dbcRepository<ProductsInCartModel, Long> {
    Mono<ProductsInCartModel> findByProductId(Long productId);

    Flux<ProductsInCartModel> findAllByOrderById();
}
