package com.example.shop.controller;

import com.example.shop.service.CartService;
import com.example.shop.service.OrderService;
import com.example.shop.utils.ViewNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OrderController {

    final OrderService orderService;
    final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @PostMapping("/buy")
    RedirectView createOrder() {
        final var orderId = orderService.createOrder();

        cartService.cleanCart();

        return new RedirectView(String.format("/orders/%s?newOrder=true", orderId));
    }

    @GetMapping("/orders")
    String getOrders(Model model) {
        final var orders = orderService.getOrders();

        model.addAttribute("orders", orders);

        return ViewNames.ORDERS;
    }

    @GetMapping("/orders/{id}")
    String getOrder(
            @PathVariable(name = "id") Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model
    ) {
        final var order = orderService.getOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return ViewNames.ORDER;
    }
}
