package com.example.main.controller;

import com.example.main.dto.ChangeCartStateItemRequest;
import com.example.main.dto.ChangeCartStateItemsRequest;
import com.example.main.dto.CreateProductDto;
import com.example.main.model.SortModel;
import com.example.main.service.CartService;
import com.example.main.service.FilesService;
import com.example.main.service.ProductService;
import com.example.main.utils.ViewNames;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.security.Principal;


@Controller
public class ProductController {

    final ProductService productService;
    final CartService cartService;
    final FilesService filesService;

    public ProductController(ProductService service, CartService cartService, FilesService filesService) {
        this.productService = service;
        this.cartService = cartService;
        this.filesService = filesService;
    }


    @GetMapping("/items/add")
    Mono<String> addProductView(Model model) {
        model.addAttribute("item", new CreateProductDto());
        return Mono.just(ViewNames.ADD_ITEM);
    }

    @PostMapping(value = "/items/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> createItem(
            @ModelAttribute("item") CreateProductDto item,
            @RequestPart("image") FilePart image
    ) {
        return filesService.upload(image)
                .flatMap(savedFileName -> {
                    String mainImagePath = "uploads/" + savedFileName;

                    return productService.createProduct(item.withImage(mainImagePath)).then(Mono.just("redirect:/items"));
                });
    }


    @GetMapping("/items/{id}")
    Mono<String> getProduct(
            Model model,
            @PathVariable Long id,
            @AuthenticationPrincipal Principal principal

    ) {
        String username = principal != null ? principal.getName() : null;

        return productService.getProduct(id, username)
                .doOnNext((item) -> {
                    model.addAttribute("item", item);
                    model.addAttribute("authorized", username != null);
                })
                .then(Mono.just(ViewNames.PRODUCT));
    }

    @GetMapping("/items")
    Mono<String> getProducts(
            Model model,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") SortModel sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @AuthenticationPrincipal Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;

        return productService.getProducts(search, sort, pageSize, pageNumber, username)
                .doOnNext((productsDto) -> {
                    model.addAttribute("items", productsDto.items());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    model.addAttribute("paging", productsDto.pagingDto());
                    model.addAttribute("authorized", username != null);
                })
                .then(Mono.just(ViewNames.PRODUCTS));
    }


    @PostMapping("/items")
    Mono<Void> changeCartStateFromItems(
            ServerWebExchange exchange,
            @ModelAttribute ChangeCartStateItemsRequest request,
            @AuthenticationPrincipal Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;

        return cartService.updateCartStateForProduct(request.id(), request.action(), username)
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
            @ModelAttribute ChangeCartStateItemRequest request,
            @AuthenticationPrincipal Principal principal

    ) {

        String username = principal != null ? principal.getName() : null;

        return cartService.updateCartStateForProduct(id, request.action(), username)
                .then(productService.getProduct(id, username))
                .doOnNext((item -> {
                    model.addAttribute("item", item);
                }))
                .then(Mono.just(ViewNames.PRODUCT));
    }
}
