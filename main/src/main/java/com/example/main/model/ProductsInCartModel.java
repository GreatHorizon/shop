package com.example.main.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table(name = "products_in_cart")
public class ProductsInCartModel {
    @Id
    private Long id;
    private int count;

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    @Column("product_id")
    private Long productId;

    public ProductsInCartModel(int count, Long productId) {
        this.count = count;
        this.productId = productId;
    }

    public ProductsInCartModel() {

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }
}
