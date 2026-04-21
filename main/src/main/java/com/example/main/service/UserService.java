package com.example.main.service;

import com.example.main.dto.RegisterUserRequest;
import com.example.main.model.UserModel;
import com.example.main.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<UserModel> register(RegisterUserRequest request) {
        return userRepository.findByUsername(request.username())
                .flatMap(existing -> Mono.<UserModel>error(new RuntimeException("User already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    UserModel user = new UserModel();
                    user.setUsername(request.username());
                    user.setPassword(passwordEncoder.encode(request.password()));
                    return userRepository.save(user);
                }));
    }
}