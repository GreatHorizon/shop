package com.example.payment.integration;

import com.example.payment.error.NotEnoughMoneyException;
import com.example.payment.model.PaymentRequest;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
class PayApiTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void makePayment_withoutToken_shouldReturn401() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(500);

        webTestClient.post()
                .uri("/pay")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void makePayment_shouldReturnNewBalance() {
        when(paymentService.pay(500)).thenReturn(Mono.just(1500));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(500);

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/pay")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("OK")
                .jsonPath("$.newBalance").isEqualTo(1500);
    }

    @Test
    void makePayment_shouldReturn4xx_whenNotEnoughMoney() {
        when(paymentService.pay(5000))
                .thenReturn(Mono.error(new NotEnoughMoneyException("Not enough money")));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(5000);

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/pay")
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}