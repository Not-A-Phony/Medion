package com.medion.hardwarestore.controller.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setup() throws Exception {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test.cart@example.com")
                .username("testcart")
                .password("password123")
                .role(com.medion.hardwarestore.domain.user.Role.CUSTOMER)
                .build();
        userRepository.save(user);

        // Create store and product
        Store store = Store.builder()
                .name("Test Store")
                .address("123 Test St")
                .latitude(0.0)
                .longitude(0.0)
                .isActive(true)
                .build();
        storeRepository.save(store);

        Product product = Product.builder()
                .name("Test Product")
                .sku("TEST-SKU-1")
                .price(new BigDecimal("10.00"))
                .currency("USD")
                .stockQuantity(10)
                .isActive(true)
                .build();
        User savedUser = userRepository.findByEmail("test.cart@example.com").orElseThrow();
        testProduct = productService.createProduct(product, store.getId(), savedUser);
    }

    @Test
    void testAddToCart() throws Exception {
        CartController.AddItemRequest request = new CartController.AddItemRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
