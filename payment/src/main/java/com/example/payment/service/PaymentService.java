package com.example.payment.service;

import com.example.payment.error.NotEnoughMoneyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentService {
    private final AtomicInteger balance = new AtomicInteger(2000);


    public Mono<Integer> getBalance() {
        return Mono.just(balance.get());
    }

    public Mono<Integer> pay(int amount) {
        if (amount > balance.get()) {
            throw new NotEnoughMoneyException("Not enough money");
        }

        final var newBalance = balance.updateAndGet((balance) -> balance - amount);

        return Mono.just(newBalance);
    }
}
