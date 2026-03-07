package com.example.shop.controller;


import com.example.shop.model.CartActionModel;
import com.example.shop.service.CartService;
import com.example.shop.utils.ViewNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
public class CartController {

    final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }


    @GetMapping("/items")
    String getCartItems(Model model) {
        final var cartItemsDto = cartService.getCartItems();

        model.addAttribute("items", cartItemsDto.items());
        model.addAttribute("total", cartItemsDto.total());

        return ViewNames.CART;
    }

    @PostMapping("/items")
    String changeCartState(
            Model model,
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "action") CartActionModel action
    ) {
        cartService.updateCartStateForProduct(id, action);

        final var cartItemsDto = cartService.getCartItems();

        model.addAttribute("items", cartItemsDto.items());
        model.addAttribute("total", cartItemsDto.total());

        return ViewNames.CART;
    }
}
