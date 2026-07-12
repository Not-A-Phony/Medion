# Medion Hardware Store API

![Medion Hardware API Banner](https://images.unsplash.com/photo-1530124566582-a618bc2615dc?w=1200&q=80)

A powerful, robust, and scalable Spring Boot RESTful API powering the **Medion Multivendor Hardware & Local Services Marketplace**. The system allows vendors to set up stores, users to browse products and services, and seamlessly handles secure checkout and geographic features.

## 🌟 Key Features

- **Multivendor Architecture:** Supports multiple store vendors, allowing them to create and manage their own hardware stores, subscriptions (Pesapal), products, and inventory.
- **Local Services Booking:** Extends beyond hardware products by offering localized services (e.g., Plumbing, Electrical, Carpentry) as specialized product offerings.
- **Store Locator & Proximity Search:** Find the nearest active hardware stores and local services based on real-time latitude/longitude haversine calculations.
- **Robust Category System:** Comprehensive category taxonomy for hardware tools, building materials, home goods, and specialized local services.
- **User Authentication & RBAC:** Secure registration and login using JWT (JSON Web Tokens) with discrete roles (e.g., `CUSTOMER`, `STORE_VENDOR`, `ADMIN`).
- **Shopping Cart & Checkout:** Fully-featured shopping cart system seamlessly transitioning to orders.
- **Payment Integrations:** Complete backend integration for Pesapal to handle both Store Subscriptions and standard customer checkouts securely.
- **Analytics & Dashboards:** Built-in endpoints providing store followers, product counts, and cumulative sales analytics for vendors and administrators.
- **Automated Seeding:** Comprehensive database seeder rapidly populates realistic test data including merchants, featured stores, localized hardware products, and dynamic category assignment.

## 🛠 Tech Stack

- **Framework:** Java 17, Spring Boot 3.2.6
- **Database:** PostgreSQL (Cloud-ready)
- **Migrations:** Flyway
- **Security:** Spring Security + JWT
- **Data Serialization:** Jackson + MapStruct
- **Environment Management:** Spring Dotenv
- **Testing:** JUnit 5, Spring Boot Test, H2 Database (In-Memory)

## 🚀 Setup & Installation

### 1. Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- **PostgreSQL Database** running locally or remotely

### 2. Database Preparation
Create a PostgreSQL database for the application.

```sql
CREATE DATABASE hardware_db;
```

### 3. Environment Variables
Create a `.env` file in the root directory of the project and populate it with your environment variables:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hardware_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_256_bits_long
JWT_EXPIRATION=86400000

# Pesapal API Credentials
PESAPAL_CONSUMER_KEY=your_pesapal_consumer_key
PESAPAL_CONSUMER_SECRET=your_pesapal_consumer_secret
PESAPAL_IS_LIVE=false
```

### 4. Build & Run
Ensure you have your `JAVA_HOME` pointed to JDK 17, then build and run the application using Maven:

```bash
# Clean and compile the project
mvn clean compile

# Run the application (This will automatically run Flyway Migrations and Database Seeding)
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

## 📚 API Endpoints Overview

- **Auth** (`/api/v1/auth`): `/register`, `/login`
- **Stores** (`/api/v1/stores`): `/my-stores`, `/{id}/products`, `/nearest`, `/{storeId}/analytics`
- **Products** (`/api/v1/products`): `/search`, `/{id}`, `/store/{storeId}`
- **Categories** (`/api/v1/categories`): `/featured`, `/root`
- **Cart** (`/api/v1/cart`): Add, update, and remove items
- **Orders** (`/api/v1/orders`): Checkout, order history
- **Admin** (`/api/v1/admin`): Superuser endpoints for marketplace management

## ☁️ Production Deployment (Render / AWS / Heroku)

This project is structured and fully ready for cloud deployment. To deploy on platforms like Render:
1. Ensure your PostgreSQL managed database is provisioned.
2. Provide all the required `.env` variables via the platform's Environment Variables settings.
3. Build the application using the standard Maven command `mvn clean package -DskipTests`.
4. Run the generated `.jar` file using `java -jar target/hardwarestore-0.0.1-SNAPSHOT.jar`.

---
*Developed with best practices in mind, this backend provides a solid, secure foundation for any multivendor e-commerce operation.*
