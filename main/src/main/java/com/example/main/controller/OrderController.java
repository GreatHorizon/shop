package com.example.main.controller;

import com.example.main.service.CartService;
import com.example.main.service.OrderService;
import com.example.main.utils.ViewNames;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.model.PaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Controller
public class OrderController {

    final OrderService orderService;
    final CartService cartService;
    final PayApi payApi;

    public OrderController(OrderService orderService, CartService cartService, PayApi payApi) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.payApi = payApi;
    }

    @PostMapping("/buy")
    Mono<Void> createOrder(
            ServerWebExchange exchange,
            @AuthenticationPrincipal Principal principal
    ) {
        final var username = principal.getName();

        return cartService.getCartPrice(username)
                .flatMap(amount ->
                        payApi.makePayment(new PaymentRequest().amount(amount))
                                .flatMap(response -> orderService.createOrder(username))
                                .flatMap(orderId ->
                                        cartService.cleanCart(username)
                                                .thenReturn(orderId)
                                )
                                .flatMap(orderId -> {
                                    var uri = UriComponentsBuilder
                                            .fromPath("/orders/{id}")
                                            .queryParam("newOrder", true)
                                            .buildAndExpand(orderId)
                                            .toUri();

                                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                    exchange.getResponse().getHeaders().setLocation(uri);

                                    return exchange.getResponse().setComplete();
                                })
                ).onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 409) {
                        var uri = UriComponentsBuilder
                                .fromPath("/cart/items")
                                .queryParam("error", "not-enough-money")
                                .build()
                                .toUri();

                        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                        exchange.getResponse().getHeaders().setLocation(uri);
                        return exchange.getResponse().setComplete();
                    }
                    return Mono.error(ex);
                });

    }

    @GetMapping("/orders")
    Mono<String> getOrders(
            Model model,
            @AuthenticationPrincipal Principal principal
    ) {
        final var username = principal.getName();
        return orderService.getOrders(username)
                .collectList()
                .doOnNext((orders) -> {
                    model.addAttribute("orders", orders);
                })
                .then(Mono.just(ViewNames.ORDERS));
    }

    @GetMapping("/orders/{id}")
    Mono<String> getOrder(
            @PathVariable(name = "id") Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model,
            @AuthenticationPrincipal Principal principal
    ) {
        final var username = principal.getName();
        return orderService.getOrder(id, username)
                .doOnNext((order) -> {
                            model.addAttribute("order", order);
                            model.addAttribute("newOrder", newOrder);
                        }
                )
                .then(Mono.just(ViewNames.ORDER));
    }
}
