package com.example.main.integration.mvc;

import com.example.main.BaseTestContainerTest;
import com.example.main.integration.utils.TestDataManager;
import com.example.main.model.UserModel;
import com.example.payment.client.api.BalanceApi;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.invoker.ApiClient;
import com.example.payment.client.model.BalanceResponse;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
public class CartControllerTest extends BaseTestContainerTest {
    private final UserModel user = new UserModel(1L, "username");
    @Autowired
    DatabaseClient databaseClient;
    @Autowired
    ApplicationContext context;

    WebTestClient webTestClient;
    @Autowired
    private TestDataManager testEntityManager;
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

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .build();
    }

    @BeforeEach
    void cleanDb() {
        databaseClient.sql("DELETE FROM products_in_cart").fetch().rowsUpdated()
                .then(databaseClient.sql("DELETE FROM orders").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM users").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM products").fetch().rowsUpdated())
                .block();
    }

    @Test
    void cart_shouldOpen() {
        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

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
        final var insertUser = testEntityManager.insertUser(user).block();


        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));


        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .get()
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
        final var insertUser = testEntityManager.insertUser(user).block();

        final var product1 = testEntityManager.insertProduct("name", null, 100).block();
        final var product2 = testEntityManager.insertProduct("name", null, 500).block();
        final var product3 = testEntityManager.insertProduct("name", null, 55).block();

        testEntityManager.addProductToCart(product1, 1, insertUser.getId()).block();
        testEntityManager.addProductToCart(product2, 1, insertUser.getId()).block();
        testEntityManager.addProductToCart(product3, 2, insertUser.getId()).block();

        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }

    @Test
    void givenEmptyCart_whenAddToCart_thenAddItem() {
        final var insertUser = testEntityManager.insertUser(user).block();

        final var product = testEntityManager.insertProduct("name", null, 100).block();

        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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

        final var insertUser = testEntityManager.insertUser(user).block();

        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));

        final var product = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product, 2, user.getId()).block();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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
        final var insertUser = testEntityManager.insertUser(user).block();

        BalanceResponse response = new BalanceResponse();
        response.setBalance(2000);

        when(balanceApi.getBalance()).thenReturn(Mono.just(response));

        final var product1 = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product1, 2, user.getId()).block();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
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