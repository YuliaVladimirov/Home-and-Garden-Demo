### WiP
This project is currently under active development, with continuous enhancements and new features being integrated.

# Home and Garden Online Shop (Demo Project)
This demo project is an improved version of my final project from a backend developer course.This web-application implements the basic functionality of an online-store backend.

## General Description
The application allows customers to select a product from the catalog, add it to the wishlist and to the cart and place an order. For administrators the application provides tools for managing the product catalog, orders, promotions and basic sales analytics.

### Key Features:
- __User Management__: Handling user registration, authentication, and authorization.
- __Product Catalog__: Managing product listings and categories.
- __Wishlist__: Allows users to save products they are interested in for later purchase.
- __Shopping Cart__: Manages the items a user intends to purchase during a shopping session.
- __Order Processing__: Facilitating order creation and status tracking.

## Detailed Project Documentation

- [Database Structure](docs/DB.md)
- The application's API in OpenAPI format is exposed via [Swagger](http://localhost:8080/swagger-ui/index.html#/)

## Technology Stack

The project was designed as Spring Boot Web Application:
- __Java 23__
- __Spring Boot 3.5.0__

The data is stored in the database:
- __PostgresSQL__

For ease of setup and development, the project uses Docker Compose to run PostgreSQL database inside the container. The database schema and initial data are consistently managed and applied using Liquibase.
