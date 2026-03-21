package com.example.shop.repository;

import com.example.shop.model.ProductModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends PagingAndSortingRepository<ProductModel, Long> {
    Page<ProductModel> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleSearch,
            String descSearch,
            Pageable pageable
    );

    ProductModel getProductModelById(long id);

    ProductModel findProductModelById(Long productId);
}
