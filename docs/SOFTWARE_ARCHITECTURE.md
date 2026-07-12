# Software Architecture

## High-Level Overview

Medion Hardware Store API follows a modern, layered monolith architecture built upon the **Spring Boot** ecosystem. It adheres to **Domain-Driven Design (DDD)** principles and **SOLID** engineering practices to ensure a highly maintainable, testable, and scalable codebase.

The system is primarily structured into modular domains (User, Store, Product, Order, Cart, Payment) connected by orchestrated Application Services.

## Architectural Layers

1. **Presentation Layer (Controllers):** 
   Found in `com.medion.hardwarestore.controller.*`, these classes define the RESTful interfaces. They handle HTTP request/response formatting, JWT extraction (via Spring Security Context), and delegate core business logic to the services.
   *Uses Java Records for immutable DTO (Data Transfer Object) projections.*

2. **Business Logic Layer (Services):**
   Found in `com.medion.hardwarestore.service.*`, this layer orchestrates the core logic. Transactions (`@Transactional`) are managed here to ensure database ACID compliance, especially during complex operations like Order Checkout where multiple aggregates (Inventory, Cart, Orders) are modified.

3. **Domain Layer (Entities & Models):**
   Found in `com.medion.hardwarestore.domain.*`, this contains JPA Entities mapping directly to PostgreSQL tables. It utilizes Hibernate ORM. Entities encapsulate both state and minor domain validations.

4. **Persistence Layer (Repositories):**
   Spring Data JPA repositories extending `JpaRepository` to abstract complex SQL queries. Where native performance or mass-updates are necessary (e.g., `DatabaseSeeder`), raw `JdbcTemplate` is utilized.

## Key Subsystems

### Multivendor Isolation
The `Store` entity forms the root of a vendor's inventory. Products, Orders, and Payments all reference a specific `Store`. Role-Based Access Control (RBAC) ensures a `STORE_VENDOR` can only mutate resources tied to their `ownerId`.

### Geospatial Search Engine
Proximity searching (finding the nearest hardware store or local service) is implemented mathematically within the JVM using the **Haversine formula** instead of relying heavily on expensive PostGIS extensions. This keeps the database lightweight while serving accurate geo-distance calculations.

### Payment Gateway Interlock (Pesapal)
The `StorePesapalService` bridges the application with the external Pesapal API. It leverages Spring's `RestTemplate` for server-to-server communication, executing a secure 2-step handshake (Token Acquisition -> Submit Order) to generate consumer payment URLs seamlessly.

### Automated Migrations & Seeding
- **Flyway:** Manages incremental schema changes (`V1__init.sql`, etc.) ensuring parity across development, staging, and production databases.
- **Seeder:** On `CommandLineRunner` startup, the `DatabaseSeeder` intelligently populates realistic testing data (Categories, Vendors, Stores, and 500+ Hardware Products) using Unsplash CDNs for rich frontend testing.

## Security Context
Security relies on stateless JWTs. 
1. `JwtAuthenticationFilter` intercepts requests.
2. The token is parsed, and the user's `Role` and `ID` are mapped into the Spring Security context.
3. Controllers use `@AuthenticationPrincipal` to safely access the requesting user's identity without database round-trips.
