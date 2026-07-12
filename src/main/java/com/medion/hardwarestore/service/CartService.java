package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.cart.Cart;
import com.medion.hardwarestore.domain.cart.CartItem;
import com.medion.hardwarestore.domain.cart.CartRepository;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    @Transactional
    public Cart getCartForUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public Cart addItemToCart(UUID productId, int quantity, User user) {
        Cart cart = getCartForUser(user);
        Product product = productService.getProductById(productId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.addItem(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(UUID itemId, int quantity, User user) {
        Cart cart = getCartForUser(user);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(quantity);
        } else {
            throw new ResourceNotFoundException("Cart item not found");
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(UUID itemId, User user) {
        Cart cart = getCartForUser(user);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
