package com.example.shop.controller;

import com.example.shop.model.CartActionModel;
import com.example.shop.model.SortModel;
import com.example.shop.service.CartService;
import com.example.shop.service.ProductService;
import com.example.shop.utils.ViewNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ProductController {

    final ProductService productService;
    final CartService cartService;

    public ProductController(ProductService service, CartService cartService) {
        this.productService = service;
        this.cartService = cartService;
    }

    @GetMapping("/items/{id}")
    String getProduct(Model model, @PathVariable Long id) {
        final var item = productService.getProduct(id);

        model.addAttribute("item", item);

        return ViewNames.PRODUCT;
    }

    @GetMapping("/items")
    String getProducts(
            Model model,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") SortModel sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber
    ) {

        final var productsDto = productService.getProducts(search, sort, pageSize, pageNumber);

        model.addAttribute("items", productsDto.items());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", productsDto.pagingDto());

        return ViewNames.PRODUCTS;
    }


    @PostMapping("/items")
    RedirectView changeCartStateFromItems(
            RedirectAttributes attributes,
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "action") CartActionModel action,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") SortModel sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber
    ) {
        cartService.updateCartStateForProduct(id, action);

        attributes.addAttribute("search", search);
        attributes.addAttribute("sort", sort);
        attributes.addAttribute("pageSize", pageSize);
        attributes.addAttribute("pageNumber", pageNumber);

        return new RedirectView(ViewNames.PRODUCTS);
    }

    @PostMapping("/items/{id}")
    String changeCartStateFromItem(
            @PathVariable(name = "id") Long id,
            Model model,
            @RequestParam(name = "action") CartActionModel action
    ) {
        cartService.updateCartStateForProduct(id, action);

        final var item = productService.getProduct(id);

        model.addAttribute("item", item);

        return ViewNames.PRODUCT;
    }
}
