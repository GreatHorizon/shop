package com.example.main.controller;

import com.example.main.dto.RegisterUserRequest;
import com.example.main.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public Mono<String> registerPage() {
        return Mono.just("register");
    }

    @PostMapping("/register")
    public Mono<String> register(@ModelAttribute RegisterUserRequest request, Model model) {
        return userService.register(request)
                .thenReturn("redirect:/login")
                .onErrorResume(ex -> {
                    model.addAttribute("errorMessage", ex.getMessage());

                    return Mono.just("register");
                });
    }
}