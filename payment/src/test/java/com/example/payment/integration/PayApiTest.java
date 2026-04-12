package com.example.payment.integration;

import com.example.payment.api.PayApi;
import com.example.payment.error.NotEnoughMoneyException;
import com.example.payment.model.PaymentRequest;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PayApi.class)
class PayApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void makePayment_shouldReturnNewBalance() {
        when(paymentService.pay(500)).thenReturn(Mono.just(1500));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(500);

        webTestClient.post()
                .uri("/pay")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("OK")
                .jsonPath("$.newBalance").isEqualTo(1500);
    }

    @Test
    void makePayment_shouldReturn5xx_whenNotEnoughMoney() {
        when(paymentService.pay(5000))
                .thenReturn(Mono.error(new NotEnoughMoneyException("Not enough money")));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(5000);

        webTestClient.post()
                .uri("/pay")
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}