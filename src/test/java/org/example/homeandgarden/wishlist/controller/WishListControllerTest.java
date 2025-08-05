package org.example.homeandgarden.wishlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.service.WishListServiceImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WishListControllerTest.TestConfig.class)
@ActiveProfiles("test")
class WishListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public WishListServiceImpl wishListService() {
            return mock(WishListServiceImpl.class);
        }
    }

    @Autowired
    private WishListServiceImpl wishListService;

    @AfterEach
    void resetMocks() {
        reset(wishListService);
    }

    @Test
    void addWishListItem_shouldReturnCreatedWishListItem_whenValidRequestAndAuthenticated() throws Exception {

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

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(validProductId)
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Product Name")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        WishListItemResponse expectedResponse = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now())
                .product(productResponse)
                .build();

        when(wishListService.addWishListItem(eq(userEmail), eq(wishListItemRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/wishlist/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishListItemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wishListItemId").exists())
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(wishListService, times(1)).addWishListItem(eq(userEmail), eq(wishListItemRequest));
    }

    @Test
    void addWishListItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(validProductId)
                .build();

        mockMvc.perform(post("/wishlist/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishListItemRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/wishlist/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any(), any());
    }


    @Test
    void addWishListItem_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

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

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .productId(invalidProductId)
                .build();

        mockMvc.perform(post("/wishlist/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/wishlist/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any(), any());
    }

    @Test
    void addWishListItem_shouldReturnBadRequest_whenMissingProductId() throws Exception {

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

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .productId(null)
                .build();

        mockMvc.perform(post("/wishlist/me")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Product id is required")))
                .andExpect(jsonPath("$.path").value("/wishlist/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any(), any());
    }

    @Test
    void removeWishListItem_shouldReturnOk_whenValidIdAndAuthenticated() throws Exception {

        String validWishListItemId = UUID.randomUUID().toString();
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
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId))
                .build();

        when(wishListService.removeWishListItem(eq(userEmail), eq(validWishListItemId))).thenReturn(expectedResponse);

        mockMvc.perform(delete("/wishlist/me/{wishListItemId}", validWishListItemId)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId)));

        verify(wishListService, times(1)).removeWishListItem(eq(userEmail), eq(validWishListItemId));
    }

    @Test
    void removeWishListItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validWishListItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/wishlist/me/{wishListItemId}", validWishListItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).removeWishListItem(any(), any());
    }

    @Test
    void removeWishListItem_shouldReturnBadRequest_whenInvalidWishListItemIdFormat() throws Exception {

        String invalidWishListItemId = "INVALID_UUID";
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

        mockMvc.perform(delete("/wishlist/me/{wishListItemId}", invalidWishListItemId)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).removeWishListItem(any(), any());
    }
}