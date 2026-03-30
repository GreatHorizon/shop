package com.example.shop.service;

import com.example.shop.dto.PagingDto;
import com.example.shop.dto.ProductDto;
import com.example.shop.dto.ProductsDto;
import com.example.shop.exception.ProductNotFoundException;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.ProductModel;
import com.example.shop.model.SortModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CartRepository cartRepository;

    public ProductService(ProductRepository repository, CartRepository cartRepository) {
        this.repository = repository;
        this.cartRepository = cartRepository;
    }

    public Mono<ProductDto> getProduct(long id) {
        return repository.getProductModelById(id)
                .map(ProductMapper::toDto)
                .flatMap(product ->
                        cartRepository.findByProductId(product.id())
                                .map(cartItem -> product.withCount(cartItem.getCount()))
                                .defaultIfEmpty(product)
                )
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Mono<ProductsDto> getProducts(String search, SortModel sort, int pageSize, int pageNumber) {
        Sort formedSort = switch (sort) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by("title");
            case PRICE -> Sort.by("price");
        };

        int offset = (pageNumber - 1) * pageSize;

        Flux<ProductModel> productsFlux = (search == null)
                ? repository.findAll(formedSort)
                : repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, formedSort
        );

        Mono<Long> totalMono = repository.count();

        return productsFlux
                .skip(offset)
                .take(pageSize)
                .map(ProductMapper::toDto)
                .flatMap(product ->
                        cartRepository.findByProductId(product.id())
                                .map(cartItem -> product.withCount(cartItem.getCount()))
                                .defaultIfEmpty(product)
                )
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
