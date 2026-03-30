package com.example.shop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "products_in_order")
public class ProductsInOrderModel {
    @Id
    private Long id;
    @Column("order_id")
    private Long orderId;

    public void setCount(int count) {
        this.count = count;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    @Column("product_id")
    private Long productId;
    private int count;

    public Long getProductId() {
        return productId;
    }

    public ProductsInOrderModel(int count, Long orderId, Long productId) {
        this.count = count;
        this.orderId = orderId;
        this.productId = productId;
    }

    public ProductsInOrderModel() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public int getCount() {
        return count;
    }

    public Long getOrderId() {
        return orderId;
    }
}
