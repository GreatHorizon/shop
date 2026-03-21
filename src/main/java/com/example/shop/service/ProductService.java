package com.example.shop.service;

import com.example.shop.dto.PagingDto;
import com.example.shop.dto.ProductDto;
import com.example.shop.dto.ProductsDto;
import com.example.shop.exception.ProductNotFoundException;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.ProductModel;
import com.example.shop.model.SortModel;
import com.example.shop.repository.ProductRepository;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductDto getProduct(long id) {
        final var productModel = repository.getProductModelById(id);

        if (productModel == null) {
            throw new ProductNotFoundException(id);
        }

        return ProductMapper.toDto(productModel);
    }

    public ProductsDto getProducts(String search, SortModel sort, int pageSize, int pageNumber) {
        final var formedSort = switch (sort) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by("title");
            case PRICE -> Sort.by("price");
        };

        final var paging = PageRequest.of(pageNumber - 1, pageSize, formedSort);

        Page<ProductModel> products;

        if (search == null) {
            products = repository.findAll(paging);
        } else {
            products =
                    repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, paging);
        }

        final var dtoList = products.map(ProductMapper::toDto);
        final var listWithPartition = ListUtils.partition(dtoList.toList(), 3);
        final var pagingDto = new PagingDto(pageSize, pageNumber, products.hasPrevious(), products.hasNext());

        return new ProductsDto(listWithPartition, pagingDto);
    }
}
