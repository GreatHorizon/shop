package com.example.main.controller;


import com.example.main.dto.ChangeCartStateItemRequest;
import com.example.main.service.CartService;
import com.example.main.utils.ViewNames;
import com.example.payment.client.api.BalanceApi;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Controller
@RequestMapping("/cart")
public class CartController {

    final CartService cartService;
    final BalanceApi balanceApi;


    public CartController(CartService cartService, BalanceApi balanceApi) {
        this.cartService = cartService;
        this.balanceApi = balanceApi;
    }


    @GetMapping("/items")
    Mono<String> getCartItems(
            @RequestParam(required = false) String error,
            Model model,
            Principal principal
    ) {
        final var username = principal.getName();

        return cartService.getCartItems(username)
                .flatMap(cartItemsDto -> {
                    model.addAttribute("items", cartItemsDto.items());
                    model.addAttribute("total", cartItemsDto.total());

                    if ("not-enough-money".equals(error)) {
                        model.addAttribute("errorMessage", "Недостаточно денег для покупки");
                    }

                    return balanceApi.getBalance()
                            .map(balanceResponse -> {
                                boolean canBuy = balanceResponse.getBalance() >= cartItemsDto.total();
                                model.addAttribute("canBuy", canBuy);

                                if (!canBuy && error == null) {
                                    model.addAttribute("errorMessage", "Недостаточно денег для покупки");
                                }

                                return ViewNames.CART;
                            })
                            .onErrorResume(ex -> {
                                model.addAttribute("canBuy", false);
                                model.addAttribute("errorMessage", "Сервис платежей временно недоступен");
                                return Mono.just(ViewNames.CART);
                            });
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    model.addAttribute("items", java.util.List.of());
                    model.addAttribute("total", 0);
                    model.addAttribute("canBuy", false);
                    return ViewNames.CART;
                }));
    }

    @PostMapping("/items")
    Mono<String> changeCartState(
            Model model,
            @ModelAttribute ChangeCartStateItemRequest request,
            Principal principal
    ) {
        model.addAttribute("canBuy", false);

        final var username = principal.getName();

        return cartService.updateCartStateForProduct(request.id(), request.action(), username)
                .then(Mono.defer(() -> cartService.getCartItems(username)))
                .flatMap(cartItemsDto -> {
                    model.addAttribute("items", cartItemsDto.items());
                    model.addAttribute("total", cartItemsDto.total());

                    return balanceApi.getBalance()
                            .map(balanceResponse -> {
                                boolean canBuy = balanceResponse.getBalance() >= cartItemsDto.total();
                                model.addAttribute("canBuy", canBuy);

                                if (!canBuy && !cartItemsDto.items().isEmpty()) {
                                    model.addAttribute("errorMessage", "Недостаточно денег для покупки");
                                }

                                return ViewNames.CART;
                            })
                            .onErrorResume(ex -> {
                                model.addAttribute("canBuy", false);
                                model.addAttribute("errorMessage", "Сервис платежей временно недоступен");
                                return Mono.just(ViewNames.CART);
                            });
                });
    }
}
