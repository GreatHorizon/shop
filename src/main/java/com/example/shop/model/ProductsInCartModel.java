package com.example.shop.model;

import jakarta.persistence.*;

@Entity(name = "products_in_cart")
public class ProductsInCartModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int count;

    @OneToOne
    @JoinColumn(name = "product_id")
    private ProductModel product;

    public ProductsInCartModel(int count, ProductModel product) {
        this.count = count;
        this.product = product;
    }

    public ProductsInCartModel() {

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ProductModel getProduct() {
        return product;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public int getTotalPrice() {
        return product.price() * count;
    }
}
