package com.medion.hardwarestore.controller.cart;

import com.medion.hardwarestore.domain.cart.Cart;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    public record CartItemDto(UUID id, UUID productId, String productName, Integer quantity, BigDecimal price) {}
    public record CartDto(UUID id, UUID userId, List<CartItemDto> items, BigDecimal totalAmount) {}
    public record AddItemRequest(UUID productId, Integer quantity) {}
    public record UpdateItemRequest(Integer quantity) {}

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal User user) {
        Cart cart = cartService.getCartForUser(user);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @RequestBody AddItemRequest request,
            @AuthenticationPrincipal User user
    ) {
        Cart cart = cartService.addItemToCart(request.productId(), request.quantity(), user);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(
            @PathVariable UUID itemId,
            @RequestBody UpdateItemRequest request,
            @AuthenticationPrincipal User user
    ) {
        Cart cart = cartService.updateItemQuantity(itemId, request.quantity(), user);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user
    ) {
        Cart cart = cartService.removeItemFromCart(itemId, user);
        return ResponseEntity.ok(mapToDto(cart));
    }

    private CartDto mapToDto(Cart cart) {
        List<CartItemDto> items = cart.getItems() == null ? List.of() : cart.getItems().stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getProduct().getPrice()
                )).toList();

        BigDecimal totalAmount = cart.getItems() == null ? BigDecimal.ZERO : cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), cart.getUser().getId(), items, totalAmount);
    }
}
