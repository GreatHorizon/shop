package com.example.main.service;

import com.example.main.model.ProductModel;
import com.example.main.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductCacheService {
    private final ProductRepository productRepository;

    ProductCacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "product", key = "#id")
    public Mono<ProductModel> getProduct(long id) {
        return productRepository.getProductModelById(id);
    }

    @Cacheable(value = "products", key = "{#search, #sort.toString(), #pageSize, #pageNumber}")
    public Flux<ProductModel> getProducts(String search, Sort sort, int pageSize, int pageNumber) {
        final var productsFlux = (search == null)
                ? productRepository.findAll(sort)
                : productRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, sort
        );

        int offset = (pageNumber - 1) * pageSize;

        return productsFlux
                .skip(offset)
                .take(pageSize);
    }
}
