# Shop Project

This is a simple e-commerce application built with **Spring Boot**, demonstrating basic CRUD operations, cart functionality, and order processing.

---

# Features

- Product listing with search and sorting
- Shopping cart management
- Order processing
- Database integration with PostgreSQL
- Unit and integration tests

---


# Getting Started

## Prerequisites

- Java 21
- Docker (for running tests with Testcontainers)

---

## Installation

### Clone the repository

```bash
git clone <repository-url>
cd shop
```

### Build the project
```bash
./mvnw clean install
```

### Run the application
```bash
./mvnw spring-boot:run
```


# Running Tests
### Run all tests
```
./mvnw test
```

### Run integration tests
```
./mvnw integration-test
```


# Database Schema

The application uses PostgreSQL with the following main tables:
- product — Stores product information
- cart — Tracks items in user carts
- order — Stores order information
- products_in_order — Junction table for order items

## Configuration

The application uses application.yaml for configuration:

- Database connection (via environment variables)
- Liquibase database migrations
- JPA settings

# Run with Docker

```bash
docker network create shop-network
```

### Db container 
```bash
docker run -d \
  --name shop-db \
  --network shop-network \
  -e POSTGRES_DB=shop \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:16
```
### App container
```bash
docker run -d \
  --name shop-app \
  --network shop-network \
  -e DB_URL=jdbc:postgresql://shop-db:5432/shop \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -p 8080:8080 \
  shop-app
```