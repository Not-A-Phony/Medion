package com.medion.hardwarestore.controller.order;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.service.OrderService;
import com.medion.hardwarestore.integration.payment.PesapalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PesapalService pesapalService;

    public record OrderItemDto(UUID productId, Integer quantity, BigDecimal price) {}
    public record OrderDto(UUID id, String status, BigDecimal totalAmount, String paymentUrl, List<OrderItemDto> items) {}

    @PostMapping("/checkout")
    public ResponseEntity<OrderDto> checkoutCart() {
        Order order = orderService.placeOrderFromCart();
        String paymentUrl = pesapalService.initiatePayment(order, order.getUser().getPhoneNumber());
        return ResponseEntity.ok(mapToDto(order, paymentUrl));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(mapToDto(order, null)); // Or retrieve the existing paymentUrl if saved in DB
    }

    private OrderDto mapToDto(Order order, String paymentUrl) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getPrice()
                )).toList();

        return new OrderDto(order.getId(), order.getStatus().name(), order.getTotalAmount(), paymentUrl, items);
    }
}
