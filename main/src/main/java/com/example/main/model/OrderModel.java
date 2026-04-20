package com.example.main.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "orders")
public class OrderModel {
    @Id private Long id;
    private Long userId;


    public OrderModel(Long userId) {
        this.userId = userId;
    }

    public OrderModel() {}

    public OrderModel(Long id, Long userId) {
        this.id = id;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
