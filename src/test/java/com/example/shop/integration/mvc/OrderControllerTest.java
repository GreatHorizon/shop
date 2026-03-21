package com.example.shop.integration.mvc;

import com.example.shop.BaseTestContainerTest;
import com.example.shop.dto.OrderDto;
import com.example.shop.integration.utils.TestDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class OrderControllerTest extends BaseTestContainerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataManager testEntityManager;

    @Test
    void givenProductInCart_whenCreateOrder_thenRedirect() throws Exception {
        final var product1 = testEntityManager.insertProduct("name", null, 100);
        final var product2 = testEntityManager.insertProduct("name", null, 500);
        final var product3 = testEntityManager.insertProduct("name", null, 55);

        testEntityManager.addProductToCart(product1, 1);
        testEntityManager.addProductToCart(product2, 1);
        testEntityManager.addProductToCart(product3, 2);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void givenProductId_whenGetOrder_thenReturnOrder() throws Exception {
        final var product = testEntityManager.insertProduct("name", null, 100);

        testEntityManager.addProductToCart(product, 1);

        mockMvc.perform(post("/buy"));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"))
                .andDo((result) -> {
                    final var order = (OrderDto) result.getModelAndView().getModel().get("order");

                    assertEquals(1, order.items().size());
                    assertEquals(100, order.totalSum());
                });
    }

    @Test
    void givenNonEmptyList_whenGetOrders_thenReturnOrders() throws Exception {
        final var product1 = testEntityManager.insertProduct("name", null, 100);
        final var product2 = testEntityManager.insertProduct("name", null, 500);

        testEntityManager.addProductToCart(product1, 1);
        testEntityManager.addProductToCart(product2, 2);

        mockMvc.perform(post("/buy"));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"))
                .andDo((result) -> {
                    final var orders = (List<OrderDto>) result.getModelAndView().getModel().get("orders");

                    assertEquals(1, orders.size());
                    assertEquals(1100, orders.getFirst().totalSum());
                });
    }
}
