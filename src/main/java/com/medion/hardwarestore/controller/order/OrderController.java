package com.medion.hardwarestore.controller.order;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.service.OrderService;
import com.medion.hardwarestore.integration.payment.PesapalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PesapalService pesapalService;

    public record OrderItemDto(UUID productId, String productName, Integer quantity, BigDecimal price) {}
    public record OrderDto(UUID id, String status, BigDecimal totalAmount, String paymentUrl, LocalDateTime createdAt, List<OrderItemDto> items) {}

    @PostMapping("/checkout")
    public ResponseEntity<OrderDto> checkoutCart(@AuthenticationPrincipal User user) {
        Order order = orderService.placeOrderFromCart(user);
        String paymentUrl = pesapalService.initiatePayment(order, order.getUser().getPhoneNumber());
        return ResponseEntity.ok(mapToDto(order, paymentUrl));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Order order = orderService.getOrderById(id);
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != com.medion.hardwarestore.domain.user.Role.ADMIN) {
            throw new com.medion.hardwarestore.exception.BusinessException("You do not have permission to view this order.");
        }
        return ResponseEntity.ok(mapToDto(order, null)); 
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(@AuthenticationPrincipal User user) {
        List<OrderDto> orders = orderService.getUserOrders(user.getId()).stream()
                .map(order -> mapToDto(order, null))
                .toList();
        return ResponseEntity.ok(orders);
    }

    private OrderDto mapToDto(Order order, String paymentUrl) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()
                )).toList();

        return new OrderDto(order.getId(), order.getStatus().name(), order.getTotalAmount(), paymentUrl, order.getCreatedAt(), items);
    }
}
