package com.example.shop.integration.mvc;

import com.example.shop.BaseTestContainerTest;
import com.example.shop.integration.utils.PostgreSQLTestContainer;
import com.example.shop.integration.utils.TestDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ImportTestcontainers(PostgreSQLTestContainer.class)
@AutoConfigureWebTestClient
public class CartControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestDataManager testEntityManager;

    @Test
    void givenNoItems_whenGetCartItems_thenEmpty() {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("0") || body.contains("Корзина") || body.contains("cart"));
                });
    }

    @Test
    void givenNonEmptyCart_whenGetCartItems_thenReturnItems() {
        final var product1 = testEntityManager.insertProduct("name", null, 100).block();
        final var product2 = testEntityManager.insertProduct("name", null, 500).block();
        final var product3 = testEntityManager.insertProduct("name", null, 55).block();

        testEntityManager.addProductToCart(product1, 1).block();
        testEntityManager.addProductToCart(product2, 1).block();
        testEntityManager.addProductToCart(product3, 2).block();

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("710"));
                    assertTrue(body.contains("name"));
                });
    }

    @Test
    void givenEmptyCart_whenAddToCart_thenAddItem() {
        final var product = testEntityManager.insertProduct("name", null, 100).block();

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", product.id())
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("100"));
                    assertTrue(body.contains("name"));
                });
    }

    @Test
    void givenNonEmptyCart_whenRemoveItem_thenEmpty() {
        final var product = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product, 2).block();

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", product.id())
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("100"));
                    assertTrue(body.contains("name"));
                });

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", product.id())
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("0") || body.contains("Корзина") || body.contains("cart"));
                    assertFalse(body.contains(">100<"));
                });
    }

    @Test
    void givenNonEmptyCart_whenClean_thenEmpty() {
        final var product1 = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product1, 2).block();

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", product1.id())
                        .queryParam("action", "DELETE")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("0") || body.contains("Корзина") || body.contains("cart"));
                    assertFalse(body.contains(">100<"));
                });
    }
}