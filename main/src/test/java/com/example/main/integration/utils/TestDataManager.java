package com.example.main.integration.utils;

import com.example.main.model.ProductModel;
import com.example.main.model.ProductsInCartModel;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TestDataManager {

    private final R2dbcEntityTemplate entityTemplate;

    public TestDataManager(R2dbcEntityTemplate entityTemplate) {
        this.entityTemplate = entityTemplate;
    }

    public Mono<ProductModel> insertProduct(String name, String description, Integer price) {
        ProductModel product = new ProductModel();

        product.setTitle(name != null ? name : "Product 1");
        product.setDescription(description != null ? description : "desc");
        product.setMainImagePath("path");
        product.setPrice(price != null ? price : 1000);

        return entityTemplate.insert(ProductModel.class)
                .using(product);
    }

    public Mono<ProductsInCartModel> addProductToCart(ProductModel product, int count) {
        ProductsInCartModel productInCartModel = new ProductsInCartModel(count, product.getId());

        return entityTemplate.insert(ProductsInCartModel.class)
                .using(productInCartModel);
    }
}
