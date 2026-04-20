package com.example.main.service;

import com.example.main.dto.CreateProductDto;
import com.example.main.dto.PagingDto;
import com.example.main.dto.ProductDto;
import com.example.main.dto.ProductsDto;
import com.example.main.exception.ProductNotFoundException;
import com.example.main.mapper.ProductMapper;
import com.example.main.model.ProductModel;
import com.example.main.model.SortModel;
import com.example.main.repository.CartRepository;
import com.example.main.repository.ProductRepository;
import com.example.main.repository.UserRepository;
import org.apache.commons.collections4.ListUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ProductService {
    private final ProductCacheService productCacheService;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductCacheService productCacheService,
            ProductRepository productRepository,
            CartRepository cartRepository,
            UserRepository userRepository
    ) {
        this.productCacheService = productCacheService;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Mono<ProductModel> createProduct(CreateProductDto product) {
        return productRepository.save(
                new ProductModel(
                        product.title(),
                        product.description(),
                        product.mainImagePath(),
                        product.price()
                )
        );
    }

    public Mono<ProductDto> getProduct(long id, String username) {
        return productCacheService.getProduct(id)
                .map(ProductMapper::toDto)
                .flatMap(product -> {
                            if (username == null) {
                                return Mono.just(product);
                            }

                            return userRepository.findByUsername(username).flatMap(user -> cartRepository.findByProductIdAndUserId(product.id(), user.getId())
                                    .map(cartItem -> product.withCount(cartItem.getCount()))
                                    .defaultIfEmpty(product));
                        }
                )
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Mono<ProductsDto> getProducts(
            String search,
            SortModel sort,
            int pageSize,
            int pageNumber,
            String username
    ) {
        Sort formedSort = switch (sort) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by("title");
            case PRICE -> Sort.by("price");
        };

        Mono<Long> totalMono = productRepository.count();


        return productCacheService.getProducts(search, formedSort, pageSize, pageNumber)
                .map(ProductMapper::toDto)
                .flatMap(product -> {
                    if (username == null) {
                        return Mono.just(product);
                    }

                    return userRepository.findByUsername(username)
                            .flatMap(user ->
                                    cartRepository.findByProductIdAndUserId(product.id(), user.getId())
                                            .map(cartItem -> product.withCount(cartItem.getCount()))
                                            .defaultIfEmpty(product)
                            )
                            .defaultIfEmpty(product);
                })
                .collectList()
                .zipWith(totalMono)
                .map(tuple -> {
                    List<ProductDto> dtoList = tuple.getT1();
                    long total = tuple.getT2();

                    var partitioned = ListUtils.partition(dtoList, 3);

                    boolean hasPrevious = pageNumber > 1;
                    boolean hasNext = ((long) pageNumber * pageSize) < total;

                    var pagingDto = new PagingDto(pageSize, pageNumber, hasPrevious, hasNext);

                    return new ProductsDto(partitioned, pagingDto);
                });
    }
}
