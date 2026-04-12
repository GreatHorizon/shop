package com.example.main.integration.mvc;

import com.example.main.BaseTestContainerTest;
import com.example.main.integration.utils.TestDataManager;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
//@ImportTestcontainers(PostgresContainerHolder.class)
@AutoConfigureWebTestClient
public class OrderControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestDataManager testEntityManager;


    @Test
    void givenProductInCart_whenCreateOrder_thenRedirect() {
        paymentMockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "status": "SUCCESS"
                    }
                    """));

        final var product1 = testEntityManager.insertProduct("name", null, 100).block();
        final var product2 = testEntityManager.insertProduct("name", null, 500).block();
        final var product3 = testEntityManager.insertProduct("name", null, 55).block();

        testEntityManager.addProductToCart(product1, 1).block();
        testEntityManager.addProductToCart(product2, 1).block();
        testEntityManager.addProductToCart(product3, 2).block();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void givenProductId_whenGetOrder_thenReturnOrder() {
        paymentMockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "status": "SUCCESS"
                    }
                    """));

        final var product = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product, 1).block();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("100"));
                    assertTrue(body.contains("name"));
                });
    }

    @Test
    void givenNonEmptyList_whenGetOrders_thenReturnOrders() {
        paymentMockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "status": "SUCCESS"
                    }
                    """));

        final var product1 = testEntityManager.insertProduct("name", null, 100).block();
        final var product2 = testEntityManager.insertProduct("name", null, 500).block();

        testEntityManager.addProductToCart(product1, 1).block();
        testEntityManager.addProductToCart(product2, 2).block();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("1100")));
    }
}