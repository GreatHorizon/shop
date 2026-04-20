package com.example.main.integration.mvc;

import com.example.main.BaseTestContainerTest;
import com.example.main.integration.utils.TestDataManager;
import com.example.main.model.UserModel;
import com.example.payment.client.api.BalanceApi;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.invoker.ApiClient;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
public class OrderControllerTest extends BaseTestContainerTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private DatabaseClient databaseClient;

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

    @Autowired
    ApplicationContext context;


    final UserModel user = new UserModel(1L, "username");

    @BeforeEach
    void cleanDb() {
        databaseClient.sql("DELETE FROM products_in_cart").fetch().rowsUpdated()
                .then(databaseClient.sql("DELETE FROM orders").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM users").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM products").fetch().rowsUpdated())
                .block();
    }

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .build();
    }


    @Test
    void givenProductInCart_whenCreateOrder_thenRedirect() {
        final var insertUser = testEntityManager.insertUser(user).block();

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

        testEntityManager.addProductToCart(product1, 1, insertUser.getId()).block();
        testEntityManager.addProductToCart(product2, 1, insertUser.getId()).block();
        testEntityManager.addProductToCart(product3, 2, insertUser.getId()).block();

        var payResponse = new com.example.payment.client.model.PaymentResponse();
        payResponse.setStatus("SUCCESS");

        when(payApi.makePayment(any(com.example.payment.client.model.PaymentRequest.class)))
                .thenReturn(Mono.just(payResponse));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void givenProductId_whenGetOrder_thenReturnOrder() {
        final var insertUser = testEntityManager.insertUser(user).block();
        
        paymentMockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "status": "SUCCESS"
                    }
                    """));

        final var product = testEntityManager.insertProduct("name", null, 100).block();

        testEntityManager.addProductToCart(product, 1, insertUser.getId()).block();

        var payResponse = new com.example.payment.client.model.PaymentResponse();
        payResponse.setStatus("SUCCESS");

        when(payApi.makePayment(any(com.example.payment.client.model.PaymentRequest.class)))
                .thenReturn(Mono.just(payResponse));


        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .get()
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
        final var insertUser = testEntityManager.insertUser(user).block();

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

        testEntityManager.addProductToCart(product1, 1, insertUser.getId()).block();
        testEntityManager.addProductToCart(product2, 2, insertUser.getId()).block();

        var payResponse = new com.example.payment.client.model.PaymentResponse();
        payResponse.setStatus("SUCCESS");

        when(payApi.makePayment(any(com.example.payment.client.model.PaymentRequest.class)))
                .thenReturn(Mono.just(payResponse));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                insertUser.getUsername(),
                                "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("1100")));
    }
}