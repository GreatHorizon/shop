package com.example.shop.controller;

import com.example.shop.dto.ChangeCartStateItemRequest;
import com.example.shop.dto.ChangeCartStateItemsRequest;
import com.example.shop.model.SortModel;
import com.example.shop.service.CartService;
import com.example.shop.service.ProductService;
import com.example.shop.utils.ViewNames;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Controller
public class ProductController {

    final ProductService productService;
    final CartService cartService;

    public ProductController(ProductService service, CartService cartService) {
        this.productService = service;
        this.cartService = cartService;
    }

    @GetMapping("/items/{id}")
    Mono<String> getProduct(Model model, @PathVariable Long id) {
        return productService.getProduct(id)
                .doOnNext((item) -> {
                    model.addAttribute("item", item);
                })
                .then(Mono.just(ViewNames.PRODUCT));
    }

    @GetMapping("/items")
    Mono<String> getProducts(
            Model model,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") SortModel sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber
    ) {
        return productService.getProducts(search, sort, pageSize, pageNumber)
                .doOnNext((productsDto) -> {
                    model.addAttribute("items", productsDto.items());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    model.addAttribute("paging", productsDto.pagingDto());
                })
                .then(Mono.just(ViewNames.PRODUCTS));
    }


    @PostMapping("/items")
    Mono<Void> changeCartStateFromItems(
            ServerWebExchange exchange,
            @ModelAttribute ChangeCartStateItemsRequest request
    ) {
        return cartService.updateCartStateForProduct(request.id(), request.action())
                .then(Mono.defer(() -> {
                    var uri = UriComponentsBuilder.fromPath("/items")
                            .queryParam("search", request.search())
                            .queryParam("sort", request.sort())
                            .queryParam("pageSize", request.pageSize())
                            .queryParam("pageNumber", request.pageNumber())
                            .build()
                            .toUri();

                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(uri);

                    return exchange.getResponse().setComplete();
                }));
    }

    @PostMapping("/items/{id}")
    Mono<String> changeCartStateFromItem(
            Model model,
            @PathVariable(name = "id") Long id,
            @ModelAttribute ChangeCartStateItemRequest request
    ) {
        return cartService.updateCartStateForProduct(id, request.action())
                .then(productService.getProduct(id))
                .doOnNext((item -> {
                    model.addAttribute("item", item);
                }))
                .then(Mono.just(ViewNames.PRODUCT));
    }
}
