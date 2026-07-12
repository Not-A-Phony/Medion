# Medion API Documentation

## Overview
The Medion Hardware Store API is a RESTful web service built with Spring Boot. It uses JSON for serialization, relies on standard HTTP methods (GET, POST, PUT, DELETE), and requires JWT (JSON Web Token) for secured endpoints.

---

## Authentication (`/api/v1/auth`)

### 1. Register User
`POST /api/v1/auth/register`
Creates a new user account.

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "password": "strongPassword123",
  "role": "CUSTOMER" // OR "STORE_VENDOR"
}
```

**Response:**
Returns an `AuthResponse` containing a JWT `accessToken` and user profile details.

### 2. Login User
`POST /api/v1/auth/login`
Authenticates a user and returns a token.

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "strongPassword123"
}
```

---

## Stores (`/api/v1/stores`)

### 1. Get All Active Stores
`GET /api/v1/stores`
Retrieves a list of all fully approved and active stores.

### 2. Create Store (Secured: STORE_VENDOR)
`POST /api/v1/stores`
Registers a new store for the authenticated vendor. Returns the store details and a `paymentUrl` for Pesapal subscription if applicable.

### 3. Get Nearest Store
`GET /api/v1/stores/nearest?lat={latitude}&lon={longitude}`
Finds the geographically closest active store using the Haversine formula.

### 4. Get Store Products
`GET /api/v1/stores/{storeId}/products`
Retrieves all products registered under a specific store.

---

## Products (`/api/v1/products`)

### 1. Get All Products
`GET /api/v1/products`
Retrieves all products from active stores.

### 2. Search Products
`GET /api/v1/products/search?q={query}`
Searches products by name or description.

---

## Shopping Cart (`/api/v1/cart`)

*(All cart endpoints require a valid JWT token)*

### 1. Get Current Cart
`GET /api/v1/cart`
Retrieves the active cart for the authenticated user.

### 2. Add Item to Cart
`POST /api/v1/cart/items`

**Request Body:**
```json
{
  "productId": "uuid-of-product",
  "quantity": 2
}
```

### 3. Clear Cart
`DELETE /api/v1/cart`
Removes all items from the user's active cart.

---

## Orders & Checkout (`/api/v1/orders`)

*(All order endpoints require a valid JWT token)*

### 1. Checkout
`POST /api/v1/orders/checkout`
Converts the active cart into an Order, verifies stock, updates inventory, and generates a Pesapal `paymentUrl`.

**Request Body:**
```json
{
  "shippingAddress": "123 Main St",
  "deliveryMethod": "STANDARD"
}
```

---

## Administration (`/api/v1/admin`)

*(Requires `ADMIN` role)*

### 1. Get All Stores (Including Pending)
`GET /api/v1/admin/stores`

### 2. Approve Store
`POST /api/v1/admin/stores/{id}/approve`

### 3. Reject Store
`POST /api/v1/admin/stores/{id}/reject`

---

*For interactive testing, please import the included Postman collection into your workspace.*
