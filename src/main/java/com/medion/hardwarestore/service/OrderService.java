package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.cart.Cart;
import com.medion.hardwarestore.domain.cart.CartItem;
import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.order.OrderItem;
import com.medion.hardwarestore.domain.order.OrderRepository;
import com.medion.hardwarestore.domain.order.OrderStatus;
import com.medion.hardwarestore.domain.store.SubscriptionType;
import com.medion.hardwarestore.domain.user.User;
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
    public Order placeOrderFromCart(User user) {
        Cart cart = cartService.getCartForUser(user);

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .store(cart.getItems().get(0).getProduct().getStore())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            BigDecimal platformCommission = BigDecimal.ZERO;
            if (cartItem.getProduct().getStore().getSubscriptionType() == SubscriptionType.COMMISSION) {
                BigDecimal commissionRate = cartItem.getProduct().getStore().getCommissionRate().divide(BigDecimal.valueOf(100));
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
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
