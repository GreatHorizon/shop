package com.example.payment.service;

import com.example.payment.error.NotEnoughMoneyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentService {
    private int balance = 2000;


    public Mono<Integer> getBalance() {
        return Mono.just(balance);
    }

    public Mono<Integer> pay(int amount) {
        if (amount > balance) {
            throw new NotEnoughMoneyException("Not enough money");
        }

        balance = balance - amount;

        return Mono.just(balance);
    }
}
