package com.example.main.integration.mvc;

import com.example.main.BaseTestContainerTest;
import com.example.main.integration.utils.TestDataManager;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureWebTestClient
public class CartControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;


    @Autowired
    private TestDataManager testEntityManager;

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    ApplicationContext context;

    @Test
    void checkLiquibaseBean() {
        String[] beanNames = context.getBeanDefinitionNames();

        Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("liquibase"))
                .sorted()
                .forEach(System.out::println);
    }

    @Test
    void checkLiquibaseBean2() {
        Map<String, SpringLiquibase> beans = context.getBeansOfType(SpringLiquibase.class);
        System.out.println("Liquibase beans: " + beans);
        assertThat(beans.isEmpty()).isFalse();
    }

    @Test
    void checkChangelogVisible() {
        var resource = Thread.currentThread()
                .getContextClassLoader()
                .getResource("db/changelog/db.changelog-master.yaml");

        System.out.println("CHANGELOG RESOURCE = " + resource);
        assertThat(resource).isNotNull();
    }


    @Test
    void checkLiquibaseTables() {
        Integer liquibaseTables = databaseClient.sql("""
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_name IN ('databasechangelog', 'databasechangeloglock')
        """)
                .map(row -> row.get(0, Integer.class))
                .one()
                .block();

        System.out.println("Liquibase meta tables count = " + liquibaseTables);

        Integer productsTable = databaseClient.sql("""
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_name = 'products'
        """)
                .map(row -> row.get(0, Integer.class))
                .one()
                .block();

        System.out.println("Products table count = " + productsTable);
    }

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
                        .queryParam("id", product.getId())
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
                        .queryParam("id", product.getId())
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
                        .queryParam("id", product.getId())
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
                        .queryParam("id", product1.getId())
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