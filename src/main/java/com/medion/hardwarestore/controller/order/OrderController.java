package com.medion.hardwarestore.controller.order;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.order.OrderStatus;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.exception.BusinessException;
import com.medion.hardwarestore.exception.MpesaException;
import com.medion.hardwarestore.service.OrderService;
import com.medion.hardwarestore.integration.payment.MpesaGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OrderController handles order creation, checkout, and order retrieval.
 * Implements CLAUDE.md spec sections 8, 9 for error handling and validation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MpesaGatewayService mpesaService;
    private final com.medion.hardwarestore.domain.order.OrderRepository orderRepository;

    public record OrderItemDto(
            UUID productId,
            String productName,
            Integer quantity,
            BigDecimal price
    ) {}

    public record OrderDto(
            UUID id,
            String status,
            BigDecimal totalAmount,
            String mpesaStatus,
            LocalDateTime createdAt,
            List<OrderItemDto> items
    ) {}

    /**
     * Standard error response format (CLAUDE.md spec §9).
     */
    public record ErrorResponse(
            String error,              // Machine-readable error code
            String message,            // Human-readable error message
            LocalDateTime timestamp    // When error occurred
    ) {}

    /**
     * Checkout cart and initiate M-Pesa payment.
     * Validates user, phone, cart, and amount before payment (CLAUDE.md spec §8).
     * Catches MpesaException and returns HTTP 402 with error details.
     *
     * @param user Authenticated user from security context
     * @return OrderDto with payment status or ErrorResponse
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutCart(@AuthenticationPrincipal User user) {
        try {
            // Validate user is authenticated (CLAUDE.md spec §8)
            if (user == null) {
                log.warn("Checkout attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                "UNAUTHORIZED",
                                "User authentication required. Please log in.",
                                LocalDateTime.now()
                        ));
            }

            // Validate user has phone number (CLAUDE.md spec §4: phone required for M-Pesa)
            if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                log.warn("Checkout attempted for user {} without phone number", user.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                "INVALID_USER",
                                "Phone number required for payment. Please update your profile with a valid phone number.",
                                LocalDateTime.now()
                        ));
            }

            // Create order from user's cart
            log.info("Processing checkout for user {}", user.getId());
            Order order = orderService.placeOrderFromCart(user);

            // Validate order was created successfully
            if (order == null || order.getId() == null) {
                log.error("Order creation failed for user {}", user.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(
                                "ORDER_CREATION_FAILED",
                                "Failed to create order. Please try again.",
                                LocalDateTime.now()
                        ));
            }

            // Validate order has items
            if (order.getItems() == null || order.getItems().isEmpty()) {
                log.warn("Order {} created but has no items", order.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                "EMPTY_CART",
                                "Cannot checkout with an empty cart. Please add items before proceeding.",
                                LocalDateTime.now()
                        ));
            }

            // Validate order amount is valid
            if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Order {} has invalid amount: {}", order.getId(), order.getTotalAmount());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                "INVALID_AMOUNT",
                                "Order total must be greater than 0. Current total: " + order.getTotalAmount(),
                                LocalDateTime.now()
                        ));
            }

            log.info("Order created successfully. ID: {}, Amount: {} KES", order.getId(), order.getTotalAmount());

            // Initiate M-Pesa STK Push (CLAUDE.md spec §5: wrap MpesaException)
            String checkoutRequestId;
            try {
                log.debug("Initiating M-Pesa payment for order {}", order.getId());
                checkoutRequestId = mpesaService.initiateStkPush(
                        user.getPhoneNumber(),
                        order.getTotalAmount().longValue(),
                        order.getId().toString()
                );

                // Save M-Pesa tracking ID to order
                order.setTrackingId(checkoutRequestId);
                orderRepository.save(order);

                log.info("M-Pesa STK Push sent successfully for order {}. CheckoutRequestID: {}",
                        order.getId(), checkoutRequestId);

                return ResponseEntity.ok(mapToDto(order, "STK_PUSH_SENT"));

            } catch (MpesaException e) {
                // M-Pesa payment initiation failed - mark order as failed (CLAUDE.md spec §8, §9)
                log.error("M-Pesa payment initiation failed for order {}: {}",
                        order.getId(), e.getMessage(), e);

                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(new ErrorResponse(
                                "PAYMENT_LINK_GENERATION_FAILED",
                                "Failed to generate payment link: " + e.getMessage(),
                                LocalDateTime.now()
                        ));
            }

        } catch (BusinessException e) {
            log.error("Business logic error during checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                            "BUSINESS_ERROR",
                            e.getMessage(),
                            LocalDateTime.now()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during checkout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "CHECKOUT_ERROR",
                            "An unexpected error occurred during checkout. Please try again later.",
                            LocalDateTime.now()
                    ));
        }
    }

    /**
     * Retrieve a specific order by ID.
     * User can only view their own orders unless they are admin.
     *
     * @param id Order ID
     * @param user Authenticated user
     * @return OrderDto or ErrorResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                "UNAUTHORIZED",
                                "User authentication required.",
                                LocalDateTime.now()
                        ));
            }

            Order order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(
                                "ORDER_NOT_FOUND",
                                "Order not found.",
                                LocalDateTime.now()
                        ));
            }

            // Check authorization
            if (!order.getUser().getId().equals(user.getId()) &&
                user.getRole() != com.medion.hardwarestore.domain.user.Role.ADMIN) {
                log.warn("Unauthorized access attempt to order {} by user {}", id, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(
                                "FORBIDDEN",
                                "You do not have permission to view this order.",
                                LocalDateTime.now()
                        ));
            }

            return ResponseEntity.ok(mapToDto(order, null));

        } catch (Exception e) {
            log.error("Error fetching order {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "ORDER_FETCH_ERROR",
                            "Failed to fetch order. Please try again.",
                            LocalDateTime.now()
                    ));
        }
    }

    /**
     * Retrieve all orders for the authenticated user.
     *
     * @param user Authenticated user
     * @return List of OrderDtos or ErrorResponse
     */
    @GetMapping
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(
                                "UNAUTHORIZED",
                                "User authentication required.",
                                LocalDateTime.now()
                        ));
            }

            List<OrderDto> orders = orderService.getUserOrders(user.getId()).stream()
                    .map(order -> mapToDto(order, null))
                    .toList();

            log.debug("Retrieved {} orders for user {}", orders.size(), user.getId());
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error fetching orders for user {}", user.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "ORDERS_FETCH_ERROR",
                            "Failed to fetch orders. Please try again.",
                            LocalDateTime.now()
                    ));
        }
    }

    /**
     * Map Order entity to OrderDto for API response.
     */
    private OrderDto mapToDto(Order order, String mpesaStatus) {
        List<OrderItemDto> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                mpesaStatus,
                order.getCreatedAt(),
                items
        );
    }
}
