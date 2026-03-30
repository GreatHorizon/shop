package com.example.shop.controller;


import com.example.shop.dto.ChangeCartStateItemRequest;
import com.example.shop.service.CartService;
import com.example.shop.utils.ViewNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/cart")
public class CartController {

    final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/items")
    Mono<String> getCartItems(Model model) {
        return cartService.getCartItems().doOnNext(cartItemsDto -> {
                    model.addAttribute("items", cartItemsDto.items());
                    model.addAttribute("total", cartItemsDto.total());
                })
                .then(Mono.just(ViewNames.CART));
    }

    @PostMapping("/items")
    Mono<String> changeCartState(
            Model model,
            @ModelAttribute ChangeCartStateItemRequest request
    ) {
        return cartService.updateCartStateForProduct(request.id(), request.action())
                .then(Mono.defer(cartService::getCartItems))
                .doOnNext(cartItemsDto -> {
                    model.addAttribute("items", cartItemsDto.items());
                    model.addAttribute("total", cartItemsDto.total());
                }).then(Mono.just(ViewNames.CART));
    }
}
