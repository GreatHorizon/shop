package com.example.main.controller;


import com.example.payment.client.api.BalanceApi;
import com.example.main.dto.ChangeCartStateItemRequest;
import com.example.main.service.CartService;
import com.example.main.utils.ViewNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    Mono<String> getCartItems(@RequestParam(required = false) String error, Model model) {
        return cartService.getCartItems()
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
                });
    }

    @PostMapping("/items")
    Mono<String> changeCartState(
            Model model,
            @ModelAttribute ChangeCartStateItemRequest request
    ) {
        model.addAttribute("canBuy", false);

        return cartService.updateCartStateForProduct(request.id(), request.action())
                .then(Mono.defer(cartService::getCartItems))
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
