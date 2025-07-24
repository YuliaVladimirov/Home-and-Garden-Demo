package org.example.homeandgarden.wishlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.shared.MessageResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
    @WithMockUser(roles = {"CLIENT"})
    void addWishListItem_shouldReturnCreatedWishListItem_whenValidRequestAndClientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(validUserId)
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

        when(wishListService.addWishListItem(eq(wishListItemRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishListItemRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wishListItemId").exists())
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.product.productId").value(validProductId))
                .andExpect(jsonPath("$.product.productName").value("Product Name"))
                .andExpect(jsonPath("$.product.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(wishListService, times(1)).addWishListItem(eq(wishListItemRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addWishListItem_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishListItemRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    void addWishListItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(validUserId)
                .productId(validProductId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishListItemRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addWishListItem_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";
        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .userId(invalidUserId)
                .productId(validProductId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addWishListItem_shouldReturnBadRequest_whenMissingUserId() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .productId(validProductId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("User id is required")))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addWishListItem_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String invalidProductId = "INVALID_UUID";

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .userId(validUserId)
                .productId(invalidProductId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addWishListItem_shouldReturnBadRequest_whenMissingProductId() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        WishListItemRequest invalidRequest = WishListItemRequest.builder()
                .userId(validUserId)
                .build();

        mockMvc.perform(post("/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Product id is required")))
                .andExpect(jsonPath("$.path").value("/wishlist"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).addWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void removeWishListItem_shouldReturnOk_whenValidIdAndClientRole() throws Exception {

        String validWishListItemId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId))
                .build();

        when(wishListService.removeWishListItem(eq(validWishListItemId))).thenReturn(expectedResponse);

        mockMvc.perform(delete("/wishlist/{wishListItemId}", validWishListItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Wishlist item with id: %s, has been removed from wishlist.", validWishListItemId)));

        verify(wishListService, times(1)).removeWishListItem(eq(validWishListItemId));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void removeWishListItem_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validWishListItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/wishlist/{wishListItemId}", validWishListItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).removeWishListItem(any());
    }

    @Test
    void removeWishListItem_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validWishListItemId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/wishlist/{wishListItemId}", validWishListItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).removeWishListItem(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void removeWishListItem_shouldReturnBadRequest_whenInvalidWishListItemIdFormat() throws Exception {

        String invalidWishListItemId = "INVALID_UUID";

        mockMvc.perform(delete("/wishlist/{wishListItemId}", invalidWishListItemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).removeWishListItem(any());
    }
}