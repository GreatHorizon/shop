package com.example.shop.integration.mvc;

import com.example.shop.BaseTestContainerTest;
import com.example.shop.integration.utils.PostgreSQLTestContainer;
import com.example.shop.integration.utils.TestDataManager;
import com.example.shop.model.OrderModel;
import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.model.ProductsInOrderModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ImportTestcontainers(PostgreSQLTestContainer.class)
@AutoConfigureWebTestClient
class ProductControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestDataManager testDataManager;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void setUp() {
        entityTemplate.delete(ProductsInOrderModel.class)
                .all()
                .then(entityTemplate.delete(OrderModel.class).all())
                .then(entityTemplate.delete(ProductsInCartModel.class).all())
                .then(entityTemplate.delete(ProductModel.class).all())
                .block();
    }

    @Test
    void givenNoItems_whenGetProducts_thenEmpty() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertFalse(body.contains("Product 1"));
                    assertFalse(body.contains("Product 2"));
                });
    }

    @Test
    void givenTwoItems_whenGetProducts_thenReturnBothProducts() {
        testDataManager.insertProduct("Product 2", null, 1000).block();
        testDataManager.insertProduct("Product 1", null, 200).block();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 1"));
                    assertTrue(body.contains("Product 2"));
                });
    }

    @Test
    void givenAlphaSort_whenGetProducts_thenReturnSortedPage() {
        testDataManager.insertProduct("Product 2", null, 1000).block();
        testDataManager.insertProduct("Product 1", null, 200).block();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .queryParam("sort", "ALPHA")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 1"));
                    assertTrue(body.contains("Product 2"));
                });
    }

    @Test
    void givenPriceSort_whenGetProducts_thenReturnSortedPage() {
        testDataManager.insertProduct("Product 2", null, 1000).block();
        testDataManager.insertProduct("Product 1", null, 200).block();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .queryParam("sort", "PRICE")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 1"));
                    assertTrue(body.contains("Product 2"));
                });
    }

    @Test
    void givenSearch_whenGetProducts_thenFilterByTitleOrDesc() {
        testDataManager.insertProduct("Product 2", null, 1000).block();
        testDataManager.insertProduct("Product 1", null, 200).block();
        testDataManager.insertProduct("Product 3", "2", 200).block();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .queryParam("search", "2")
                        .queryParam("sort", "PRICE")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 2"));
                    assertTrue(body.contains("Product 3"));
                    assertFalse(body.contains("Product 1"));
                });
    }

    @Test
    void givenExistingId_whenGetProduct_thenReturnProductPage() {
        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient.get()
                .uri("/items/{id}", model.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 2"));
                    assertTrue(body.contains("desc test"));
                    assertTrue(body.contains("1000"));
                    assertTrue(body.contains("path"));
                });
    }

    @Test
    void givenUnknownId_whenGetProduct_thenReturn404() {
        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void givenIdAndPlus_whenChangeCartStateFromItems_thenRedirectIncrement() {
        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(
                        "id=" + model.getId()
                                + "&action=PLUS"
                                + "&search=test"
                                + "&sort=PRICE"
                                + "&pageNumber=1"
                                + "&pageSize=5"
                )
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(
                        "Location",
                        "/items?search=test&sort=PRICE&pageSize=5&pageNumber=1"
                );

        ProductsInCartModel cartItem = entityTemplate
                .select(ProductsInCartModel.class)
                .matching(Query.query(Criteria.where("product_id").is(model.getId())))
                .one()
                .block();

        assertNotNull(cartItem);
        assertEquals(model.getId(), cartItem.getProductId());
        assertEquals(1, cartItem.getCount());
    }

    @Test
    void givenIdAndRemove_whenChangeCartStateFromItems_thenRedirectWithDecrement() {
        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + model.getId() + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();

        ProductsInCartModel created = entityTemplate
                .select(ProductsInCartModel.class)
                .matching(Query.query(Criteria.where("product_id").is(model.getId())))
                .one()
                .block();

        assertNotNull(created);
        assertEquals(1, created.getCount());

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(
                        "id=" + model.getId()
                                + "&action=MINUS"
                                + "&search=test"
                                + "&sort=PRICE"
                                + "&pageNumber=1"
                                + "&pageSize=5"
                )
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(
                        "Location",
                        "/items?search=test&sort=PRICE&pageSize=5&pageNumber=1"
                );

        ProductsInCartModel cartItemAfterMinus = entityTemplate
                .select(ProductsInCartModel.class)
                .matching(Query.query(Criteria.where("product_id").is(model.getId())))
                .one()
                .block();

        assertNull(cartItemAfterMinus);
    }

    @Test
    void givenIdAndPlus_whenChangeCartStateFromItem_thenRenderProductPage() {
        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient.post()
                .uri("/items/{id}?action=PLUS", model.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Product 2"));
                });

        ProductsInCartModel cartItem = entityTemplate
                .select(ProductsInCartModel.class)
                .matching(Query.query(Criteria.where("product_id").is(model.getId())))
                .one()
                .block();

        assertNotNull(cartItem);
        assertEquals(model.getId(), cartItem.getProductId());
        assertEquals(1, cartItem.getCount());
    }
}