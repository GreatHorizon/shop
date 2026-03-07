package com.example.shop.unit.service;

import com.example.shop.model.OrderModel;
import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.model.ProductsInOrderModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.OrderRepository;
import com.example.shop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = {
                OrderService.class,
                OrderRepository.class,
                CartRepository.class
        }
)
public class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CartRepository cartRepository;


    @BeforeEach
    void resetAll() {
        Mockito.reset(orderRepository);
        Mockito.reset(cartRepository);
    }

    @Test
    void createOrder_shouldCreateOrderFromCartItems() {
        // Given
        ProductModel product1 = new ProductModel();
        product1.setId(1L);
        product1.setTitle("Product 1");
        product1.setPrice(100);

        ProductModel product2 = new ProductModel();
        product2.setId(2L);
        product2.setTitle("Product 2");
        product2.setPrice(200);

        ProductsInCartModel cartItem1 = new ProductsInCartModel(2, product1);
        ProductsInCartModel cartItem2 = new ProductsInCartModel(1, product2);

        List<ProductsInCartModel> cartItems = List.of(cartItem1, cartItem2);

        when(cartRepository.findAll()).thenReturn(cartItems);

        OrderModel savedOrder = new OrderModel();
        savedOrder.setId(1L);

        when(orderRepository.save(any(OrderModel.class))).thenReturn(savedOrder);

        Long orderId = orderService.createOrder();

        assertEquals(1L, orderId);

        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        verify(orderRepository).save(orderCaptor.capture());

        OrderModel capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(2, capturedOrder.getProducts().size());

        ProductsInOrderModel orderProduct1 = capturedOrder.getProductsInOrderModels().getFirst();
        assertEquals(2, orderProduct1.getCount());
        assertEquals(product1, orderProduct1.getProduct());
        assertEquals(capturedOrder, orderProduct1.getOrder());

        ProductsInOrderModel orderProduct2 = capturedOrder.getProductsInOrderModels().get(1);
        assertEquals(1, orderProduct2.getCount());
        assertEquals(product2, orderProduct2.getProduct());
        assertEquals(capturedOrder, orderProduct2.getOrder());
    }

    @Test
    void getOrders_shouldReturnAllOrdersAsDtos() {
        // Given
        OrderModel order1 = new OrderModel(
                1L,
                List.of(
                        new ProductsInOrderModel(
                                100,
                                new ProductModel(
                                        1L,
                                        "Product 1",
                                        "some desc",
                                        "path",
                                        100000
                                )
                        )
                )
        );

        final var productInCart = new ProductsInCartModel(100, order1.getProducts().getFirst());

        order1.getProducts().getFirst().setProductInCartReference(productInCart);

        final var order2 = new OrderModel(2L,
                List.of(
                        new ProductsInOrderModel(
                                100,
                                new ProductModel(
                                        1L,
                                        "Product 1",
                                        "some desc",
                                        "path",
                                        100000
                                )
                        )
                )
        );

        List<OrderModel> orders = List.of(order1, order2);

        when(orderRepository.findAll()).thenReturn(orders);

        final var ordersDtos = orderService.getOrders();

        assertEquals(2, ordersDtos.size());
        assertEquals(1L, ordersDtos.get(0).id());
        assertEquals(2L, ordersDtos.get(1).id());
        assertEquals(100, ordersDtos.get(0).items().getFirst().count());

        verify(orderRepository).findAll();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrder_shouldReturnOrderByIdAsDto() {
        OrderModel order = new OrderModel(
                1L,
                List.of(
                        new ProductsInOrderModel(
                                100,
                                new ProductModel(
                                        1L,
                                        "Product 1",
                                        "some desc",
                                        "path",
                                        100000
                                )
                        )
                )
        );

        final var productInCart = new ProductsInCartModel(100, order.getProducts().getFirst());

        order.getProducts().getFirst().setProductInCartReference(productInCart);

        when(orderRepository.getOrderById(order.getId())).thenReturn(order);

        final var ordersDto = orderService.getOrder(order.getId());

        assertEquals(1L, ordersDto.id());
        assertEquals(1, ordersDto.items().size());
        assertEquals(100, ordersDto.items().getFirst().count());
    }

}
