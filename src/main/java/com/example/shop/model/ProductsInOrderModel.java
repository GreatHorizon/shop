package com.example.shop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products_in_order")
public class ProductsInOrderModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderModel order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductModel product;

    @Column(name = "count")
    private int count;

    public ProductsInOrderModel(int count, ProductModel product) {
        this.count = count;
        this.product = product;
    }

    public ProductsInOrderModel() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderModel getOrder() {
        return order;
    }

    public void setOrder(OrderModel order) {
        this.order = order;
    }

    public ProductModel getProduct() {
        return product;
    }

    public void setProduct(ProductModel product) {
        this.product = product;
    }

    public int getTotalPrice() {
        return product.price() * count;
    }

    public int getCount() {
        return count;
    }
}
