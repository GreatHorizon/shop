package com.example.shop.service;

import com.example.shop.dto.OrderDto;
import com.example.shop.mapper.OrderMapper;
import com.example.shop.model.OrderModel;
import com.example.shop.model.ProductsInOrderModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    public OrderService(OrderRepository repository, CartRepository cartRepository) {
        this.orderRepository = repository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public Long createOrder() {
        final var cartProducts = cartRepository.findAll();

        final var order = new OrderModel();

        for (var cartItem : cartProducts) {
            ProductsInOrderModel item = new ProductsInOrderModel(
                    cartItem.getCount(),
                    cartItem.getProduct()
            );

            item.setOrder(order);

            order.addProduct(item);
        }

        return orderRepository.save(order).getId();
    }

    public List<OrderDto> getOrders() {
        final var orders = orderRepository.findAll();

        return orders.stream().map(OrderMapper::toDto).toList();
    }

    public OrderDto getOrder(Long id) {
        final var order = orderRepository.getOrderById(id);

        return OrderMapper.toDto(order);
    }

}
