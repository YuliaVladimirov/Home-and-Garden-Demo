
# Home and Garden Online Shop (Demo Project)
This demo project is an improved version of my final project from a backend developer course. It is a Spring Boot web application that implements the basic functionality of an online store backend.

---

## 📖 General Description
The application allows customers to select a product from the catalog, add it to the wishlist and to the cart and place an order.<br>
For administrators the application provides tools for managing the product catalog, orders, promotions and basic sales analytics.

### ⭐ Key Features:
- __User Management__: Registration, authentication, and authorization.
- __Product Catalog__: Managing product listings and categories.
- __Wishlist__: Users can save products for later purchase.
- __Shopping Cart__: Tracks intended purchases during a shopping session.
- __Order Processing__: Facilitates order creation and status tracking.

---

## 🛠️ Technology Stack
The project was designed as Spring Boot Web Application and uses the following technologies:
- __Java 23__
- __Spring Boot 3.x__
- __Spring Data JPA__ (Hibernate)
- __PostgeSQL__ (default DB, configurable)
- __Liquibase__ (for database schema and migrations)
- __Lombok__ (for boilerplate reduction)
- __Docker / Docker Compose:__ (to simplify launching the application and the database)
- __Hurl__ (for HTTP-based integration testing)
- __GitHub Actions__ (for CI/CD pipeline)

---

## 📝 Project Documentation
- [Database Structure](docs/DB.md)
- REST API docs: exposed via [Swagger](http://localhost:8080/swagger-ui/index.html#/)

---

## ⚙️ Running the Application with Docker Compose

You need three files in your working directory to start the application in Docker Compose:

1. [`compose.yml`](compose.yml) from this repository,
1. `.env` with the database credentials, for example:

    ```[bash]
    DB_USER="USERNAME"
    DB_PASSWORD="PASSWORD"
    ```

3. `secret.properties` which holds sensitive credentials (DB, JWT secrets, mailing, etc.).<br>Use the provided [`secret.properties.example`](secret.properties.example) - rename it to `secret.properties` and fill in your own values.

To start the application, simply execute:

```[bash]
docker compose up
```

This will launch two containers - one for the PostgreSQL database and one more for the application itself. Once started, the application can be manipulated with Swagger - http://localhost:8080/swagger-ui/index.html#

---

## 📝 Setting Up in Development

If you want to use or modify the application source code, here is how you prepare the project for your IDE  _(IntelliJ IDEA, Eclipse, VS Code)_:

1. Clone the repository:

    ```[bash]
    git clone https://github.com/YuliaVladimirov/Home-and-Garden-Demo.git
    ```
2. Open the cloned project in your IDE.
2. Start an empty PostgreSQL database:
    - independently with your own PostgreSQL instalation OR
    - with the provided docker compose file (in the root directory of the project):
    
        ```[bash]
        docker compose up -d db
        ```

2. Create the files .env ans secret.properties as described in the previous section (__Running the Application with Docker Compose__).

Now you can use your IDE functionality to launch the application. Upon the first start the database schema and test data will be applied automatically by __Liquibase__.

> ⚠️ __Note__<br>
> `secret.properties` and `.env` should not be committed to Git!<br>


### Building a .jar file

```[bash]
mvn package
```
Maven will run the tests before building the jar package. Some of this tests require the database - make sure the PostgreSQL is running before you build the application.

### Running integration tests with Hurl
Along with unit tests and Spring Boot integration tests, the project also includes integration tests written for [Hurl](https://hurl.dev/). These tests are automatically executed in CI/CD, but you can also run them locally with the Hurl CLI (in the root directory of the project):

```[bash]
hurl --test --verbose --color tests/*.hurl
```

## ✅ Continuous Integration & Delivery
This project is set up with GitHub Actions:

On each push to `main`:

- Maven builds and tests the application (unit tests + integration tests).
- A Docker image is built and tested with Hurl.
- If tests pass, the image is pushed to the GitHub Container Registry.

You can find the published images in [ghcr.io/yuliavladimirov/home-and-garden-demo](https://ghcr.io/yuliavladimirov/home-and-garden-demo).
