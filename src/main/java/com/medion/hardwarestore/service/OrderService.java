package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.cart.Cart;
import com.medion.hardwarestore.domain.cart.CartItem;
import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.order.OrderItem;
import com.medion.hardwarestore.domain.order.OrderRepository;
import com.medion.hardwarestore.domain.order.OrderStatus;
import com.medion.hardwarestore.domain.store.SubscriptionType;
import com.medion.hardwarestore.exception.BusinessException;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Transactional
    public Order placeOrderFromCart() {
        Cart cart = cartService.getCartForCurrentUser();

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assuming all items are from the same store for simplicity here,
        // or just taking the store from the first product
        Order order = Order.builder()
                .user(cart.getUser())
                .store(cart.getItems().get(0).getProduct().getStore())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            BigDecimal platformCommission = BigDecimal.ZERO;
            if (cartItem.getProduct().getStore().getSubscriptionType() == SubscriptionType.COMMISSION) {
                BigDecimal commissionRate = BigDecimal.valueOf(cartItem.getProduct().getStore().getCommissionRate()).divide(BigDecimal.valueOf(100));
                platformCommission = cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                        .multiply(commissionRate);
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getProduct().getPrice())
                    .platformCommission(platformCommission)
                    .build();
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(cart);

        return savedOrder;
    }

    public List<Order> getUserOrders(UUID userId) {
        // Find orders by user id logic
        // This is simplified. Normally you'd add findByUserId in OrderRepository
        return orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .toList();
    }

    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
