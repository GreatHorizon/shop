package com.example.main.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "orders")
public class OrderModel {
    @Id private Long id;


    public OrderModel() {
    }

    public OrderModel(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
