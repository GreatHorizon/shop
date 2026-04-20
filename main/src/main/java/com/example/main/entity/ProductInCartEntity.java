package com.example.main.entity;

import com.example.main.model.ProductModel;

public class ProductInCartEntity {
    private Long id;

    public int getCount() {
        return count;
    }

    private final int count;
    private final ProductModel product;

    public ProductInCartEntity(Long id, int count, ProductModel product) {
        this.id = id;
        this.count = count;
        this.product = product;
    }

    public ProductModel getProduct() {
        return product;
    }

    public int getTotalPrice() {
        return product.getPrice() * count;
    }

}
