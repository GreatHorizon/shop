//package com.example.main.integration.utils;
//
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//public abstract class BaseTestContainerTest {
//
//    protected static final PostgreSQLContainer<?> postgres =
//            new PostgreSQLContainer<>("postgres:16-alpine")
//                    .withDatabaseName("test")
//                    .withUsername("test")
//                    .withPassword("test");
//
//    static {
//        postgres.start();
//    }
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.r2dbc.url", () ->
//                "r2dbc:postgresql://localhost:" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName());
//        registry.add("spring.r2dbc.username", postgres::getUsername);
//        registry.add("spring.r2dbc.password", postgres::getPassword);
//
//        registry.add("spring.liquibase.enabled", () -> true);
//        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
//        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
//        registry.add("spring.liquibase.user", postgres::getUsername);
//        registry.add("spring.liquibase.password", postgres::getPassword);
//    }
//}