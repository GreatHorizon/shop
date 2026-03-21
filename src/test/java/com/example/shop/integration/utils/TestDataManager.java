package com.example.shop.integration.utils;

import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class TestDataManager {
    final EntityManager entityManager;

    public TestDataManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ProductModel insertProduct(String name, String description, Integer price) {
        final var product = new ProductModel();

        product.setTitle(name != null ? name : "Product 1");
        product.setDescription(description != null ? description : "desc");
        product.setMainImagePath("path");
        product.setPrice(price != null ? price : 1000);

        entityManager.persist(product);

        return product;
    }

    public void addProductToCart(ProductModel product, int count) {
        final var productInCartModel = new ProductsInCartModel(count, product);

        entityManager.persist(productInCartModel);
    }
}
