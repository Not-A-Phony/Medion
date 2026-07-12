package com.medion.hardwarestore.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.controller.cart.CartController;
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

import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
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
    private UserRepository userRepository;

    @Autowired
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setup() throws Exception {
        User user = User.builder()
                .firstName("Test")
                .lastName("Order")
                .email("test.order@example.com")
                .username("testorder")
                .password("password123")
                .role(com.medion.hardwarestore.domain.user.Role.CUSTOMER)
                .build();
        userRepository.save(user);

        Store store = Store.builder()
                .name("Order Store")
                .address("123 Order St")
                .latitude(0.0)
                .longitude(0.0)
                .isActive(true)
                .status(com.medion.hardwarestore.domain.store.StoreStatus.APPROVED)
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
        User savedUser = userRepository.findByEmail("test.order@example.com").orElseThrow();
        testProduct = productService.createProduct(product, store.getId(), savedUser);
    }

    @Test
    @WithUserDetails(value = "testorder", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testCheckoutOrder() throws Exception {
        CartController.AddItemRequest request = new CartController.AddItemRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/checkout"))
                .andExpect(status().isOk());
    }
}
