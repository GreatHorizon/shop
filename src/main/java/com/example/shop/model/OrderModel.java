package com.example.shop.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProductsInOrderModel> productsInOrderModels = new ArrayList<>();

    public OrderModel() {
    }

    public OrderModel(Long id, List<ProductsInOrderModel> productsInOrderModels) {
        this.id = id;
        this.productsInOrderModels = productsInOrderModels;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void addProduct(ProductsInOrderModel productsInCartModel) {
        productsInOrderModels.add(productsInCartModel);
        productsInCartModel.setOrder(this);
    }

    public List<ProductModel> getProducts() {
        return productsInOrderModels.stream().map(ProductsInOrderModel::getProduct).toList();
    }

    public List<ProductsInOrderModel>  getProductsInOrderModels() {
        return productsInOrderModels;
    }

    public long getTotalSum() {
        return productsInOrderModels.stream()
                .map(ProductsInOrderModel::getTotalPrice)
                .reduce(Integer::sum)
                .orElse(0);
    }
}
