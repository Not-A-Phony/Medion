# Hardware Store API

A robust Spring Boot RESTful API for a modern hardware store. The system provides features for user authentication, product management, shopping carts, checkout, and geographical store lookup.

## Features

- **User Authentication:** Secure registration and login using JWT (JSON Web Tokens).
- **Store Locator:** Find the nearest hardware stores based on user latitude and longitude.
- **Product Management:** Browse active products, manage stock levels, and organize products by store.
- **Shopping Cart System:** Fully featured shopping cart enabling users to add items, manage quantities, and retrieve their current cart.
- **Order Management & Checkout:** Seamlessly transition a shopping cart into an order, automatically verifying stock and preparing for payment integrations (like M-Pesa).
- **Automated Database Migrations:** Flyway is used to manage PostgreSQL database schemas seamlessly.

## Tech Stack

- **Framework:** Java 17, Spring Boot 3.2.6
- **Database:** PostgreSQL
- **Migrations:** Flyway
- **Security:** Spring Security + JWT
- **Object Mapping:** MapStruct
- **Environment Management:** Spring Dotenv
- **Testing:** JUnit 5, Spring Boot Test, H2 Database (for in-memory testing)

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL database

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database for the application.

```sql
CREATE DATABASE hardware_store;
```

### 2. Environment Variables

Create a `.env` file in the root directory of the project and populate it with your environment variables:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hardware_store
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_256_bits_long
JWT_EXPIRATION=86400000
```

### 3. Build & Run

Ensure you have your `JAVA_HOME` pointed to JDK 17, then build and run the application using Maven:

```bash
# Clean and compile the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

## Testing the API

A complete Postman collection is provided in the root directory: `HardwareStoreAPI.postman_collection.json`. 

You can import this collection directly into Postman to easily test all endpoints including Authentication, Store Locator, Cart Operations, and Order Checkout.

*Note: Make sure to extract the `accessToken` from the Login/Register response and paste it into your Postman collection variables to authorize protected requests.*

## Project Structure

- `controller`: REST endpoints for Auth, Cart, Orders, Products, and Stores.
- `domain`: JPA Entities (User, Cart, Product, Order, Store) and Repositories.
- `service`: Core business logic and transaction management.
- `security`: JWT filter, utility classes, and Security Configuration.
- `exception`: Global Exception Handling for seamless error responses.
- `config`: Additional Spring configurations.

## Database Migrations

This project uses **Flyway** for automated database migrations. 
- Migration files are located in `src/main/resources/db/migration`.
- Upon starting the application, Flyway will automatically execute any pending migrations (`V1__init_schema.sql`, `V2__orders_cart_payments_delivery.sql`, etc.) to keep your schema up to date.

## Production Deployment (Render)

This project is structured and ready for cloud deployment. To deploy on platforms like Render:
1. Ensure your PostgreSQL managed database is provisioned.
2. Provide all the required `.env` variables via the platform's Environment Variables settings.
3. Build the application using the standard Maven command `mvn clean package`.
4. Run the generated `.jar` file using `java -jar target/hardwarestore-0.0.1-SNAPSHOT.jar`.
