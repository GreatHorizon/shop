package com.example.payment.unit;

import com.example.payment.error.NotEnoughMoneyException;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    void getBalance_shouldReturnInitialBalance() {
        Integer balance = paymentService.getBalance().block();
        assertEquals(2000, balance);
    }

    @Test
    void pay_shouldDecreaseBalance() {
        Integer newBalance = paymentService.pay(500).block();
        assertEquals(1500, newBalance);

        Integer balance = paymentService.getBalance().block();
        assertEquals(1500, balance);
    }

    @Test
    void pay_shouldThrowWhenNotEnoughMoney() {
        NotEnoughMoneyException ex = assertThrows(
                NotEnoughMoneyException.class,
                () -> paymentService.pay(3000)
        );

        assertEquals("Not enough money", ex.getMessage());
    }

    @Test
    void pay_shouldHandleSeveralPayments() {
        assertEquals(1700, paymentService.pay(300).block());
        assertEquals(1500, paymentService.pay(200).block());
        assertEquals(1500, paymentService.getBalance().block());
    }
}