package com.example.shop.integration.mvc;

import com.example.shop.BaseTestContainerTest;
import com.example.shop.integration.utils.TestDataManager;
import com.example.shop.utils.ViewNames;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class CartControllerTest extends BaseTestContainerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataManager testEntityManager;


    @Test
    void givenNoItems_whenGetCartItems_thenEmpty() throws Exception {
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(0)));
    }

    @Test
    void givenNonEmptyCart_whenGetCartItems_thenReturnItems() throws Exception {
        final var product1 = testEntityManager.insertProduct("name", null, 100);
        final var product2 = testEntityManager.insertProduct("name", null, 500);
        final var product3 = testEntityManager.insertProduct("name", null, 55);

        testEntityManager.addProductToCart(product1, 1);
        testEntityManager.addProductToCart(product2, 1);
        testEntityManager.addProductToCart(product3, 2);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(3)))
                .andExpect(model().attribute("total", equalTo(710)));
    }

    @Test
    void givenEmptyCart_whenAddToCart_thenAddItem() throws Exception {
        final var product = testEntityManager.insertProduct("name", null, 100);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(product.id()))
                        .param("action", "PLUS")
                )
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(model().attribute("total", equalTo(100)));
    }

    @Test
    void givenNonEmptyCart_whenRemoveItem_thenEmpty() throws Exception {
        final var product = testEntityManager.insertProduct("name", null, 100);

        testEntityManager.addProductToCart(product, 2);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(product.id()))
                        .param("action", "MINUS")
                )
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(model().attribute("total", equalTo(100)));

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(product.id()))
                        .param("action", "MINUS")
                )
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(0)))
                .andExpect(model().attribute("total", equalTo(0)));
    }

    @Test
    void givenNonEmptyCart_whenClean_thenEmpty() throws Exception {
        final var product1 = testEntityManager.insertProduct("name", null, 100);

        testEntityManager.addProductToCart(product1, 2);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(product1.id()))
                        .param("action", "DELETE")
                )
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.CART))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", hasSize(0)))
                .andExpect(model().attribute("total", equalTo(0)));
    }
}
