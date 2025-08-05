package org.example.homeandgarden.cart.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.cart.service.CartServiceImpl;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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


    // 🔐 Self-access endpoints — available only to the authenticated user (operates on their own data)

    @Test
    void addCartItem_shouldReturnCreatedCartItem_whenValidRequestAndAuthenticated() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest cartItemRequest = CartItemCreateRequest.builder()
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

        when(cartService.addCartItem(eq(userEmail), eq(cartItemRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(cartService, times(1)).addCartItem(eq(userEmail), eq(cartItemRequest));
    }


    @Test
    void addCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest cartItemRequest = CartItemCreateRequest.builder()
                .productId(validProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void addCartItem_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String invalidProductId = "INVALID_UUID";

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .productId(invalidProductId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void addCartItem_shouldReturnBadRequest_whenMissingProductId() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .quantity(1)
                .productId(null)
                .build();

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Product id is required")))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void addCartItem_shouldReturnBadRequest_whenQuantityIsNull() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .productId(validProductId)
                .quantity(null)
                .build();

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity is required")))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void addCartItem_shouldReturnBadRequest_whenQuantityIsLessThanOne() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .productId(validProductId)
                .quantity(0)
                .build();

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity must be at least 1")))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void addCartItem_shouldReturnBadRequest_whenQuantityIsGreaterThanOneThousand() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String validProductId = UUID.randomUUID().toString();

        CartItemCreateRequest invalidRequest = CartItemCreateRequest.builder()
                .productId(validProductId)
                .quantity(1001)
                .build();

        mockMvc.perform(post("/cart/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity cannot exceed 1000")))
                .andExpect(jsonPath("$.path").value("/cart/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).addCartItem(any(), any());
    }

    @Test
    void updateCartItem_shouldReturnUpdatedCartItem_whenValidRequestAndAuthenticated() throws Exception {

        String userEmail = "user@example.com";
        String validCartItemId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

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

        when(cartService.updateCartItem(eq(userEmail), eq(validCartItemId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/cart/me/{cartItemId}", validCartItemId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(validCartItemId))
                .andExpect(jsonPath("$.quantity").value(updateRequest.getQuantity()))
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(cartService, times(1)).updateCartItem(eq(userEmail), eq(validCartItemId), eq(updateRequest));
    }


    @Test
    void updateCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        CartItemUpdateRequest request = CartItemUpdateRequest.builder()
                .quantity(1)
                .build();

        mockMvc.perform(patch("/cart/me/{cartItemId}", validCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    void updateCartItem_shouldReturnBadRequest_whenInvalidCartItemIdFormat() throws Exception {

        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        String invalidCartItemId = "INVALID_UUID";
        CartItemUpdateRequest updateRequest = CartItemUpdateRequest.builder()
                .quantity(1)
                .build();

        mockMvc.perform(patch("/cart/me/{cartItemId}", invalidCartItemId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsNull() throws Exception {

        String userEmail = "user@example.com";
        String validCartItemId = UUID.randomUUID().toString();

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(null)
                .build();

        mockMvc.perform(patch("/cart/me/{cartItemId}", validCartItemId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity is required")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsLessThanOne() throws Exception {

        String userEmail = "user@example.com";
        String validCartItemId = UUID.randomUUID().toString();

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(0)
                .build();

        mockMvc.perform(patch("/cart/me/{cartItemId}", validCartItemId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity must be at least 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    void updateCartItem_shouldReturnBadRequest_whenQuantityIsGreaterThanOneThousand() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();
        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        CartItemUpdateRequest invalidRequest = CartItemUpdateRequest.builder()
                .quantity(1001)
                .build();

        mockMvc.perform(patch("/cart/me/{cartItemId}", validCartItemId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Quantity cannot exceed 1000")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    void removeCartItem_shouldReturnOk_whenValidIdAndAuthenticated() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();
        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("Cart item with id: %s, has been removed from cart.", validCartItemId))
                .build();

        when(cartService.removeCarItem(eq(userEmail), eq(validCartItemId))).thenReturn(expectedResponse);

        mockMvc.perform(delete("/cart/me/{cartItemId}", validCartItemId)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Cart item with id: %s, has been removed from cart.", validCartItemId)));

        verify(cartService, times(1)).removeCarItem(eq(userEmail), eq(validCartItemId));
    }

    @Test
    void removeCartItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validCartItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/cart/me/{cartItemId}", validCartItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).removeCarItem(any(), any());
    }

    @Test
    void removeCartItem_shouldReturnBadRequest_whenInvalidCartItemIdFormat() throws Exception {

        String invalidCartItemId = "INVALID_UUID";
        String userEmail = "user@example.com";

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(userEmail)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(existingUser);

        mockMvc.perform(delete("/cart/me/{cartItemId}", invalidCartItemId)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).removeCarItem(any(), any());
    }
}