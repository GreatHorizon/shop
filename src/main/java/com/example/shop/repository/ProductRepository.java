package com.example.shop.repository;

import com.example.shop.model.ProductModel;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends R2dbcRepository<ProductModel, Long> {
    Flux<ProductModel> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleSearch,
            String descSearch,
            Sort sort
    );

    Mono<ProductModel> getProductModelById(long id);

    Mono<ProductModel> findProductModelById(Long productId);
}
