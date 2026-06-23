package com.medion.hardwarestore.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.controller.auth.RegisterRequest;
import com.medion.hardwarestore.controller.cart.CartController;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
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
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductService productService;

    private String token;
    private Product testProduct;

    @BeforeEach
    void setup() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("Order")
                .email("test.order@example.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        token = objectMapper.readTree(responseStr).get("accessToken").asText();

        Store store = Store.builder()
                .name("Order Store")
                .address("123 Order St")
                .latitude(0.0)
                .longitude(0.0)
                .isActive(true)
                .build();
        storeRepository.save(store);

        Product product = Product.builder()
                .name("Order Product")
                .sku("ORDER-SKU-1")
                .price(new BigDecimal("15.00"))
                .currency("USD")
                .stockQuantity(10)
                .isActive(true)
                .build();
        testProduct = productService.createProduct(product, store.getId());
    }

    @Test
    void testCheckoutOrder() throws Exception {
        CartController.AddItemRequest request = new CartController.AddItemRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
