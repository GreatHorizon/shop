package com.example.shop.repository;

import com.example.shop.model.ProductsInCartModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<ProductsInCartModel, Long> {
    Optional<ProductsInCartModel> findByProduct_Id(Long productId);

    List<ProductsInCartModel> findAllByOrderById();
}
