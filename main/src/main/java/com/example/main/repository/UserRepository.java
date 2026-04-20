package com.example.main.repository;

import com.example.main.model.UserModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserModel, Long>{
    Mono<UserModel> findByUsername(String username);
}
