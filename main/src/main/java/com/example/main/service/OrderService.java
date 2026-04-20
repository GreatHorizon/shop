package com.example.main.service;

import com.example.main.dto.OrderDto;
import com.example.main.dto.ProductDto;
import com.example.main.mapper.ProductMapper;
import com.example.main.model.OrderModel;
import com.example.main.model.ProductsInOrderModel;
import com.example.main.repository.CartRepository;
import com.example.main.repository.OrderRepository;
import com.example.main.repository.ProductRepository;
import com.example.main.repository.ProductsInOrderRepository;
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


    public OrderService(OrderRepository repository, CartRepository cartRepository, ProductRepository productRepository, ProductsInOrderRepository productInOrderRepository) {
        this.orderRepository = repository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.productInOrderRepository = productInOrderRepository;
    }

    @Transactional
    public Mono<Long> createOrder() {
        return orderRepository.save(new OrderModel())
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
                );
    }


    public Flux<OrderDto> getOrders() {
        return orderRepository.findAll()
                .flatMap(orderModel ->
                        getProducts(orderModel)
                                .collectList()
                                .map(products -> createOrderDto(orderModel, products)
                                )
                );
    }

    public Mono<OrderDto> getOrder(Long id) {
        return orderRepository.getOrderById(id)
                .flatMap(orderModel -> getProducts(orderModel)
                        .collectList()
                        .map(products -> createOrderDto(orderModel, products))
                );
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
