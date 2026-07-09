package com.medion.hardwarestore.controller.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.service.ProductService;
import com.medion.hardwarestore.controller.auth.RegisterRequest;
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
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    private String token;
    private Product testProduct;

    @BeforeEach
    void setup() throws Exception {
        // Register user to get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test.cart@example.com")
                .username("testcart")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        token = objectMapper.readTree(responseStr).get("accessToken").asText();

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
        User user = userRepository.findByEmail("test.cart@example.com").orElseThrow();
        testProduct = productService.createProduct(product, store.getId(), user);
    }

    @Test
    void testAddToCart() throws Exception {
        CartController.AddItemRequest request = new CartController.AddItemRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
