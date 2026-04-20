package com.example.payment;

import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.service.PaymentService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PayApi implements com.example.payment.api.PayApi {
    private final PaymentService paymentService;

    public PayApi(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<PaymentResponse> makePayment(Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest.flatMap(req ->
                paymentService.pay(req.getAmount()).map(res -> {
                    final var response = new PaymentResponse();

                    response.setStatus("OK");
                    response.setNewBalance(res);

                    return response;
                })
        );
    }
}
