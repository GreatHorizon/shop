package com.example.shop.repository;


import com.example.shop.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, Integer> {
    OrderModel getOrderById(long id);
}