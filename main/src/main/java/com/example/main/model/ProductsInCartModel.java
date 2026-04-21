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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Column("user_id")
    private Long userId;

    public ProductsInCartModel(int count, Long productId, Long userId) {
        this.count = count;
        this.productId = productId;
        this.userId = userId;
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
