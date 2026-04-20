package com.example.main.unit.service;

import com.example.main.dto.OrderDto;
import com.example.main.model.OrderModel;
import com.example.main.model.ProductModel;
import com.example.main.model.ProductsInCartModel;
import com.example.main.model.ProductsInOrderModel;
import com.example.main.repository.CartRepository;
import com.example.main.repository.OrderRepository;
import com.example.main.repository.ProductRepository;
import com.example.main.repository.ProductsInOrderRepository;
import com.example.main.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = OrderService.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private ProductsInOrderRepository productsInOrderRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @BeforeEach
    void resetAll() {
        Mockito.reset(orderRepository, cartRepository, productsInOrderRepository, productRepository);
    }

    @Test
    void createOrder_shouldCreateOrderFromCartItems() {
        ProductsInCartModel cartItem1 = new ProductsInCartModel();
        cartItem1.setId(1L);
        cartItem1.setProductId(1L);
        cartItem1.setCount(2);

        ProductsInCartModel cartItem2 = new ProductsInCartModel();
        cartItem2.setId(2L);
        cartItem2.setProductId(2L);
        cartItem2.setCount(1);

        ProductModel product1 = new ProductModel();
        product1.setId(1L);
        product1.setTitle("Product 1");
        product1.setDescription("desc 1");
        product1.setMainImagePath("path 1");
        product1.setPrice(100);

        ProductModel product2 = new ProductModel();
        product2.setId(2L);
        product2.setTitle("Product 2");
        product2.setDescription("desc 2");
        product2.setMainImagePath("path 2");
        product2.setPrice(200);

        OrderModel savedOrder = new OrderModel(1L);

        when(cartRepository.findAll())
                .thenReturn(Flux.just(cartItem1, cartItem2));

        when(productRepository.findById(1L))
                .thenReturn(Mono.just(product1));
        when(productRepository.findById(2L))
                .thenReturn(Mono.just(product2));

        when(orderRepository.save(any(OrderModel.class)))
                .thenReturn(Mono.just(savedOrder));

        when(productsInOrderRepository.save(any(ProductsInOrderModel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(orderService.createOrder())
                .expectNext(1L)
                .verifyComplete();

        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        verify(orderRepository).save(orderCaptor.capture());

        OrderModel capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);

        ArgumentCaptor<ProductsInOrderModel> productsCaptor =
                ArgumentCaptor.forClass(ProductsInOrderModel.class);

        verify(productsInOrderRepository, times(2)).save(productsCaptor.capture());

        var savedProducts = productsCaptor.getAllValues()
                .stream()
                .sorted(Comparator.comparing(ProductsInOrderModel::getProductId))
                .toList();

        assertEquals(2, savedProducts.size());

        assertEquals(1L, savedProducts.get(0).getProductId());
        assertEquals(2, savedProducts.get(0).getCount());

        assertEquals(2L, savedProducts.get(1).getProductId());
        assertEquals(1, savedProducts.get(1).getCount());

        verify(cartRepository).findAll();
        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
    }

    @Test
    void getOrders_shouldReturnAllOrdersAsDtos() {
        OrderModel order1 = new OrderModel(1L);
        OrderModel order2 = new OrderModel(2L);

        ProductsInOrderModel order1Item = new ProductsInOrderModel();
        order1Item.setId(101L);
        order1Item.setOrderId(1L);
        order1Item.setProductId(10L);
        order1Item.setCount(2);

        ProductsInOrderModel order2Item = new ProductsInOrderModel();
        order2Item.setId(102L);
        order2Item.setOrderId(2L);
        order2Item.setProductId(20L);
        order2Item.setCount(3);

        ProductModel product1 = new ProductModel();
        product1.setId(10L);
        product1.setTitle("Product 1");
        product1.setDescription("some desc");
        product1.setMainImagePath("path");
        product1.setPrice(1000);

        ProductModel product2 = new ProductModel();
        product2.setId(20L);
        product2.setTitle("Product 2");
        product2.setDescription("some desc");
        product2.setMainImagePath("path");
        product2.setPrice(500);

        when(orderRepository.findAll())
                .thenReturn(Flux.just(order1, order2));

        when(productsInOrderRepository.findByOrderId(1L))
                .thenReturn(Flux.just(order1Item));
        when(productsInOrderRepository.findByOrderId(2L))
                .thenReturn(Flux.just(order2Item));

        when(productRepository.getProductModelById(10L))
                .thenReturn(Mono.just(product1));
        when(productRepository.getProductModelById(20L))
                .thenReturn(Mono.just(product2));

        StepVerifier.create(orderService.getOrders().collectList())
                .assertNext(orderDtos -> {
                    assertEquals(2, orderDtos.size());

                    OrderDto first = orderDtos.get(0);
                    OrderDto second = orderDtos.get(1);

                    assertEquals(1L, first.id());
                    assertEquals(1, first.items().size());
                    assertEquals(2, first.items().getFirst().count());

                    assertEquals(2L, second.id());
                    assertEquals(1, second.items().size());
                    assertEquals(3, second.items().getFirst().count());
                })
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(productsInOrderRepository).findByOrderId(1L);
        verify(productsInOrderRepository).findByOrderId(2L);
        verify(productRepository).getProductModelById(10L);
        verify(productRepository).getProductModelById(20L);
    }

    @Test
    void getOrder_shouldReturnOrderByIdAsDto() {
        OrderModel order = new OrderModel(1L);

        ProductsInOrderModel orderItem = new ProductsInOrderModel();
        orderItem.setId(201L);
        orderItem.setOrderId(1L);
        orderItem.setProductId(100L);
        orderItem.setCount(100);

        ProductModel product = new ProductModel();
        product.setId(100L);
        product.setTitle("Product 1");
        product.setDescription("some desc");
        product.setMainImagePath("path");
        product.setPrice(100000);

        when(orderRepository.getOrderById(1L))
                .thenReturn(Mono.just(order));

        when(productsInOrderRepository.findByOrderId(1L))
                .thenReturn(Flux.just(orderItem));

        when(productRepository.getProductModelById(100L))
                .thenReturn(Mono.just(product));

        StepVerifier.create(orderService.getOrder(1L))
                .assertNext(orderDto -> {
                    assertEquals(1L, orderDto.id());
                    assertEquals(1, orderDto.items().size());
                    assertEquals(100, orderDto.items().getFirst().count());
                })
                .verifyComplete();

        verify(orderRepository).getOrderById(1L);
        verify(productsInOrderRepository).findByOrderId(1L);
        verify(productRepository).getProductModelById(100L);
    }
}