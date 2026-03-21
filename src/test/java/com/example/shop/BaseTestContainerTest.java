package com.example.shop;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public class BaseTestContainerTest {
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresContainerHolder::jdbcUrl);
        registry.add("spring.datasource.username", PostgresContainerHolder::username);
        registry.add("spring.datasource.password", PostgresContainerHolder::password);
    }
}
