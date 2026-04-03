package com.example.shop.controller;

import com.example.shop.service.CartService;
import com.example.shop.service.OrderService;
import com.example.shop.utils.ViewNames;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Controller
public class OrderController {

    final OrderService orderService;
    final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @PostMapping("/buy")
    Mono<Void> createOrder(ServerWebExchange exchange) {
        return orderService.createOrder()
                .flatMap(orderId -> cartService.cleanCart().thenReturn(orderId))
                .doOnNext((orderId) -> {
                    final var uri = UriComponentsBuilder
                            .fromPath(String.format("/orders/%s", orderId))
                            .queryParam("newOrder", true)
                            .build();

                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(uri.toUri());
                })
                .then(exchange.getResponse().setComplete());
    }

    @GetMapping("/orders")
    Mono<String> getOrders(Model model) {
        return orderService.getOrders()
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
            Model model
    ) {
        return orderService.getOrder(id)
                .doOnNext((order) -> {
                            model.addAttribute("order", order);
                            model.addAttribute("newOrder", newOrder);
                        }
                )
                .then(Mono.just(ViewNames.ORDER));
    }
}
