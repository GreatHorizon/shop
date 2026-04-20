package com.example.main.service;

import com.example.main.dto.OrderDto;
import com.example.main.dto.ProductDto;
import com.example.main.mapper.ProductMapper;
import com.example.main.model.OrderModel;
import com.example.main.model.ProductsInOrderModel;
import com.example.main.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductsInOrderRepository productInOrderRepository;
    private final UserRepository userRepository;


    public OrderService(OrderRepository repository, CartRepository cartRepository, ProductRepository productRepository, ProductsInOrderRepository productInOrderRepository, UserRepository userRepository) {
        this.orderRepository = repository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.productInOrderRepository = productInOrderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Mono<Long> createOrder(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> orderRepository.save(new OrderModel(user.getId()))
                        .flatMap(savedOrder ->
                                cartRepository.findAll()
                                        .flatMap(cartItem ->
                                                productRepository.findById(cartItem.getProductId())
                                                        .flatMap(product ->
                                                                productInOrderRepository.save(
                                                                        new ProductsInOrderModel(
                                                                                cartItem.getCount(),
                                                                                savedOrder.getId(),
                                                                                product.getId()
                                                                        )
                                                                )
                                                        )
                                        )
                                        .then(Mono.just(savedOrder.getId()))
                        ));
    }


    public Flux<OrderDto> getOrders(String username) {
        return userRepository
                .findByUsername(username)
                .flatMapMany(user -> orderRepository.findAll()
                        .flatMap(orderModel ->
                                getProducts(orderModel)
                                        .collectList()
                                        .map(products -> createOrderDto(orderModel, products)
                                        )
                        ));

    }

    public Mono<OrderDto> getOrder(Long orderId, String username) {
        return userRepository
                .findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalStateException("Local user not found: " + username)))
                .flatMap(user -> orderRepository.getOrderById(orderId, user.getId())
                        .flatMap(orderModel -> getProducts(orderModel)
                                .collectList()
                                .map(products -> createOrderDto(orderModel, products))
                        ));
    }

    private Flux<ProductDto> getProducts(OrderModel orderModel) {
        return
                productInOrderRepository.findByOrderId(orderModel.getId())
                        .flatMap(pio ->
                                productRepository.getProductModelById(pio.getProductId())
                                        .map(product -> ProductMapper.toDto(product, pio.getCount()))
                        );
    }

    private OrderDto createOrderDto(OrderModel model, List<ProductDto> products) {
        return new OrderDto(
                model.getId(),
                products,
                products.stream()
                        .map((productInOrderDto) -> productInOrderDto.price() * productInOrderDto.count())
                        .reduce(Integer::sum)
                        .orElse(0)

        );
    }
}
