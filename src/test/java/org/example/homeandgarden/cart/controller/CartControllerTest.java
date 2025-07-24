package org.example.homeandgarden.cart.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.cart.service.CartServiceImpl;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.shared.MessageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CartControllerTest.TestConfig.class)
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public CartServiceImpl cartService() {
            return mock(CartServiceImpl.class);
        }
    }

    @Autowired
    private CartServiceImpl cartService;

    @AfterEach
    void resetMocks() {
        reset(cartService);
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnCreatedCartItem_whenValidRequestAndClientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest cartItemRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(3)
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Product Name")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(cartItemRequest.getQuantity())
                .addedAt(Instant.now())
                .updatedAt(Instant.now())
                .product(productResponse)
                .build();

        when(cartService.addCartItem(eq(cartItemRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(cartService, times(1)).addCartItem(eq(cartItemRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addCartItem_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest cartItemRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    void addCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest cartItemRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(invalidUserId)
                .productId(validProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenMissingUserId() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .productId(validProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("User id is required")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String invalidProductId = "INVALID_UUID";

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(invalidProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenMissingProductId() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Product id is required")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenQuantityIsNull() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(null)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity is required")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenQuantityIsLessThanOne() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(0)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity must be at least 1")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCartItem_shouldReturnBadRequest_whenQuantityIsGreaterThanOneThousand() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .quantity(1001)
                .build();

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity cannot exceed 1000")))
                .andExpect(jsonPath("$.path").value("/cart"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnUpdatedCartItem_whenValidRequestAndClientRole() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        CartItemUpdateRequest updateRequest = CartItemUpdateRequest.builder()
                .quantity(5)
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Product Name")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .cartItemId(UUID.fromString(validCartItemId))
                .quantity(updateRequest.getQuantity())
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .product(productResponse)
                .build();

        when(cartService.updateCartItem(eq(validCartItemId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(validCartItemId))
                .andExpect(jsonPath("$.quantity").value(updateRequest.getQuantity()))
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(cartService, times(1)).updateCartItem(eq(validCartItemId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCartItem_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest request = CartItemUpdateRequest.builder()
                .quantity(1)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    void updateCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest request = CartItemUpdateRequest.builder()
                .quantity(1)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnBadRequest_whenInvalidCartItemIdFormat() throws Exception {

        String invalidCartItemId = "INVALID_UUID";
        CartItemUpdateRequest updateRequest = CartItemUpdateRequest.builder()
                .quantity(1)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", invalidCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsNull() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(null)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity is required")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsLessThanOne() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(0)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity must be at least 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsGreaterThanOneThousand() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(1001)
                .build();

        mockMvc.perform(patch("/cart/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity cannot exceed 1000")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void removeCartItem_shouldReturnOk_whenValidIdAndClientRole() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("Cart item with id: %s, has been removed from cart.", validCartItemId))
                .build();

        when(cartService.removeCarItem(eq(validCartItemId))).thenReturn(expectedResponse);

        mockMvc.perform(delete("/cart/{cartItemId}", validCartItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Cart item with id: %s, has been removed from cart.", validCartItemId)));

        verify(cartService, times(1)).removeCarItem(eq(validCartItemId));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void removeCartItem_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/cart/{cartItemId}", validCartItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).removeCarItem(any());
    }

    @Test
    void removeCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/cart/{cartItemId}", validCartItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).removeCarItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void removeCartItem_shouldReturnBadRequest_whenInvalidCartItemIdFormat() throws Exception {

        String invalidCartItemId = "INVALID_UUID";

        mockMvc.perform(delete("/cart/{cartItemId}", invalidCartItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).removeCarItem(any());
    }
}