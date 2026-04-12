package com.example.payment;

import com.example.payment.model.BalanceResponse;
import com.example.payment.service.PaymentService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class BalanceApi implements com.example.payment.api.BalanceApi {

    private final PaymentService paymentService;

    public BalanceApi(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @Override
    public Mono<BalanceResponse> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance().map(balance -> new BalanceResponse().balance(balance));
    }
}