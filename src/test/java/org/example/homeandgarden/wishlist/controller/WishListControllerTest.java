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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

    private static final String USER_EMAIL = "user@example.com";
    private static final User EXISTING_USER = User.builder()
            .userId(UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76"))
            .email(USER_EMAIL)
            .passwordHash("Hashed Password")
            .firstName("First Name")
            .lastName("Last Name")
            .userRole(UserRole.CLIENT)
            .isEnabled(true)
            .isNonLocked(true)
            .registeredAt(Instant.parse("2024-01-01T12:00:00Z"))
            .updatedAt(Instant.parse("2024-01-15T08:30:00Z"))
            .build();
    private static final UserDetailsImpl USER_DETAILS = new UserDetailsImpl(EXISTING_USER);


    // 🔐 Self-access endpoints — available only to the authenticated user (operates on their own data)

    @Test
    void addWishListItem_shouldReturnCreatedWishListItem_whenValidRequestAndAuthenticated() throws Exception {

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

        when(wishListService.addWishListItem(eq(USER_EMAIL), eq(wishListItemRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/wishlist/me")
                        .with(user(USER_DETAILS))
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

        verify(wishListService, times(1)).addWishListItem(eq(USER_EMAIL), eq(wishListItemRequest));
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

        String invalidProductId = "INVALID_UUID";

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .productId(invalidProductId)
                .build();

        mockMvc.perform(post("/wishlist/me")
                        .with(user(USER_DETAILS))
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

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .productId(null)
                .build();

        mockMvc.perform(post("/wishlist/me")
                        .with(user(USER_DETAILS))
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

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId))
                .build();

        when(wishListService.removeWishListItem(eq(USER_EMAIL), eq(validWishListItemId))).thenReturn(expectedResponse);

        mockMvc.perform(delete("/wishlist/me/{wishListItemId}", validWishListItemId)
                        .with(user(USER_DETAILS))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId)));

        verify(wishListService, times(1)).removeWishListItem(eq(USER_EMAIL), eq(validWishListItemId));
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

        mockMvc.perform(delete("/wishlist/me/{wishListItemId}", invalidWishListItemId)
                        .with(user(USER_DETAILS))
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