package com.example.main.integration.mvc;

import com.example.main.BaseTestContainerTest;
import com.example.main.integration.utils.TestDataManager;
import com.example.main.model.*;
import com.example.main.service.ProductCacheService;
import com.example.payment.client.api.BalanceApi;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.invoker.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebTestClient
class ProductControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    private TestDataManager testDataManager;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private BalanceApi balanceApi;

    @MockitoBean
    private PayApi payApi;

    @MockitoBean
    private ApiClient paymentApiClient;

    @MockitoBean
    private WebClient webClient;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    ApplicationContext context;

    private final UserModel user = new UserModel(1L, "username");

    @Autowired
    private TestDataManager testEntityManager;


    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();

        entityTemplate.delete(ProductsInOrderModel.class)
                .all()
                .then(entityTemplate.delete(OrderModel.class).all())
                .then(entityTemplate.delete(ProductsInCartModel.class).all())
                .then(entityTemplate.delete(ProductModel.class).all())
                .block();

        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .build();
    }


    @BeforeEach
    void cleanDb() {
        databaseClient.sql("DELETE FROM products_in_cart").fetch().rowsUpdated()
                .then(databaseClient.sql("DELETE FROM users").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM users").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM products").fetch().rowsUpdated())
                .block();
    }

    @Test
    void givenProductsCachedByRoute_whenDeletedFromDb_thenGetProductsRouteReturnsCachedProducts() {
        ProductModel first = testDataManager.insertProduct("Cached Product 1", "desc 1", 100).block();
        ProductModel second = testDataManager.insertProduct("Cached Product 2", "desc 2", 200).block();

        assertNotNull(first);
        assertNotNull(second);

        String redisKey = "products::null,UNSORTED,5,1";

        redisTemplate.delete(redisKey);

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
                    assertTrue(body.contains("Cached Product 1"));
                    assertTrue(body.contains("Cached Product 2"));
                });

        Boolean hasKey = redisTemplate.hasKey(redisKey);
        assertEquals(Boolean.TRUE, hasKey);

        entityTemplate.delete(ProductModel.class)
                .all()
                .block();

        Long countInDb = entityTemplate.select(ProductModel.class)
                .count()
                .block();

        assertEquals(0L, countInDb);

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
                    assertTrue(body.contains("Cached Product 1"));
                    assertTrue(body.contains("Cached Product 2"));
                });
    }

    @Test
    void givenProductCachedByRoute_whenDeletedFromDb_thenGetProductRouteReturnsCachedProduct() {
        ProductModel model = testDataManager
                .insertProduct("Cached product", "cached desc", 777)
                .block();

        assertNotNull(model);
        assertNotNull(model.getId());

        String redisKey = "product::" + model.getId();

        redisTemplate.delete(redisKey);

        webTestClient.get()
                .uri("/items/{id}", model.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Cached product"));
                    assertTrue(body.contains("cached desc"));
                    assertTrue(body.contains("777"));
                });

        Boolean keyExists = redisTemplate.hasKey(redisKey);
        assertEquals(Boolean.TRUE, keyExists);

        entityTemplate.delete(ProductModel.class)
                .matching(Query.query(Criteria.where("id").is(model.getId())))
                .all()
                .block();

        ProductModel existsInDbAfterDelete = entityTemplate
                .select(ProductModel.class)
                .matching(Query.query(Criteria.where("id").is(model.getId())))
                .one()
                .block();

        assertNull(existsInDbAfterDelete);

        webTestClient.get()
                .uri("/items/{id}", model.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("Cached product"));
                    assertTrue(body.contains("cached desc"));
                    assertTrue(body.contains("777"));
                });
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
        final var insertUser = testEntityManager.insertUser(user).block();


        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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
        final var insertUser = testEntityManager.insertUser(user).block();

        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                )).post()
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

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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
        final var insertUser = testEntityManager.insertUser(user).block();


        ProductModel model = testDataManager
                .insertProduct("Product 2", "desc test", 1000)
                .block();

        assertNotNull(model);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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