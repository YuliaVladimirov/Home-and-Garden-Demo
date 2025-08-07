package org.example.homeandgarden.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.service.CartServiceImpl;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.order.service.OrderServiceImpl;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.ChangePasswordRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.dto.UserUnregisterRequest;
import org.example.homeandgarden.user.dto.UserUpdateRequest;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.service.UserServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserControllerTest.TestConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public UserServiceImpl userService() {
            return mock(UserServiceImpl.class);
        }

        @Bean
        @Primary
        public WishListServiceImpl wishListService() {
            return mock(WishListServiceImpl.class);
        }

        @Bean
        @Primary
        public CartServiceImpl cartService() {
            return mock(CartServiceImpl.class);
        }

        @Bean
        @Primary
        public OrderServiceImpl orderService() {
            return mock(OrderServiceImpl.class);
        }

    }

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private WishListServiceImpl wishListService;

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private OrderServiceImpl orderService;

    @AfterEach
    void resetMocks() {
        reset(userService, wishListService, cartService, orderService);
    }

    private static final String USER_EMAIL = "user@example.com";
    private static final UUID USER_ID = UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76");
    private static final User EXISTING_USER = User.builder()
            .userId(USER_ID)
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
    void getMyProfile_shouldReturnUser_whenAuthenticated() throws Exception {

        UserResponse userResponse = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email(USER_EMAIL)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        when(userService.getMyProfile(eq(USER_EMAIL))).thenReturn(userResponse);

        mockMvc.perform(get("/users/me/profile", USER_EMAIL)
                        .with(user(USER_DETAILS))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("First Name"))
                .andExpect(jsonPath("$.lastName").value("Last Name"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.registeredAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).getMyProfile(eq(USER_EMAIL));
    }

    @Test
    void getMyProfile_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/users/me/profile", USER_EMAIL)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getMyProfile(eq(USER_EMAIL));
    }

    @Test
    void getMyWishListItems_shouldReturnPagedWishlistItems_whenValidParametersAndAuthenticated() throws Exception {

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        WishListItemResponse item1 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .product(product1)
                .build();

        WishListItemResponse item2 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<WishListItemResponse> content = Arrays.asList(item1, item2);

        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "addedAt");
        Page<WishListItemResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(wishListService.getMyWishListItems(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/me/wishListItems")
                        .with(user(USER_DETAILS))
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].wishListItemId").exists())
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].wishListItemId").exists())
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(wishListService, times(1)).getMyWishListItems(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"));
    }


    @Test
    void getMyWishListItems_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/users/me/wishListItems")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getMyWishListItems(any(), any(), any(), any());
    }

    @Test
    void getMyWishListItems_shouldReturnPagedWishlistItems_whenDefaultParameters() throws Exception {

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        WishListItemResponse item1 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .product(product1)
                .build();

        WishListItemResponse item2 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<WishListItemResponse> content = Arrays.asList(item1, item2);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "addedAt");
        Page<WishListItemResponse> mockPage = new PageImpl<>(content, pageRequest, 10);

        when(wishListService.getMyWishListItems(eq(USER_EMAIL), eq(10), eq(0), eq("ASC")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/users/me/wishListItems")
                        .with(user(USER_DETAILS))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].wishListItemId").exists())
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].wishListItemId").exists())
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(wishListService, times(1)).getMyWishListItems(eq(USER_EMAIL), eq(10), eq(0), eq("ASC"));
    }

    @Test
    void getMyWishListItems_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/users/me/wishListItems")
                        .with(user(USER_DETAILS))
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getMyWishListItems(any(), any(), any(), any());
    }

    @Test
    void getMyWishListItems_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/users/me/wishListItems")
                        .with(user(USER_DETAILS))
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getMyWishListItems(any(), any(), any(), any());
    }

    @Test
    void getMyWishListItems_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/users/me/wishListItems")
                        .with(user(USER_DETAILS))
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getMyWishListItems(any(), any(), any(), any());
    }

    @Test
    void getMyCartItems_shouldReturnPagedCartItems_whenValidParametersAndAuthenticated() throws Exception {

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse item1 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .product(product1)
                .build();

        CartItemResponse item2 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<CartItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "quantity");
        Page<CartItemResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(cartService.getMyCartItems(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"), eq("quantity"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "quantity")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].cartItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].cartItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(1))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(cartService, times(1)).getMyCartItems(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"), eq("quantity"));
    }

    @Test
    void getMyCartItems_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/users/me/cartItems")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getMyCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getMyCartItems_shouldReturnPagedCartItems_whenDefaultParameters() throws Exception {

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse item1 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .product(product1)
                .build();

        CartItemResponse item2 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<CartItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "addedAt");
        Page<CartItemResponse> mockPage = new PageImpl<>(content, pageRequest, 2);

        when(cartService.getMyCartItems(eq(USER_EMAIL), eq(10), eq(0), eq("ASC"), eq("addedAt")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].cartItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].cartItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(1))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(cartService, times(1)).getMyCartItems(eq(USER_EMAIL), eq(10), eq(0), eq("ASC"), eq("addedAt"));
    }

    @Test
    void getMyCartItems_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getMyCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getMyCartItems_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getMyCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getMyCartItems_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getMyCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getMyCartItems_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        mockMvc.perform(get("/users/me/cartItems")
                        .with(user(USER_DETAILS))
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be either: 'addedAt' or 'quantity'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getMyCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getMyOrders_shouldReturnPagedOrders_whenValidParametersAndAuthenticated() throws Exception {

        OrderResponse order1 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("12345")
                .city("City One")
                .phone("+123456789")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        OrderResponse order2 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("54321")
                .city("City Two")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        List<OrderResponse> content = Arrays.asList(order1, order2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "status");
        Page<OrderResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(orderService.getMyOrders(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"), eq("status"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "status")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderId").exists())
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].address").value("Address One"))
                .andExpect(jsonPath("$.content[0].zipCode").value("12345"))
                .andExpect(jsonPath("$.content[0].city").value("City One"))
                .andExpect(jsonPath("$.content[0].phone").value("+123456789"))
                .andExpect(jsonPath("$.content[0].deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.content[0].orderStatus").value(OrderStatus.PAID.name()))

                .andExpect(jsonPath("$.content[1].orderId").exists())
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].address").value("Address Two"))
                .andExpect(jsonPath("$.content[1].zipCode").value("54321"))
                .andExpect(jsonPath("$.content[1].city").value("City Two"))
                .andExpect(jsonPath("$.content[1].phone").value("+987654321"))
                .andExpect(jsonPath("$.content[1].deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.content[1].orderStatus").value(OrderStatus.CREATED.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orderService, times(1)).getMyOrders(eq(USER_EMAIL), eq(2), eq(0), eq("DESC"), eq("status"));
    }

    @Test
    void getMyOrders_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/users/me/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getMyOrders(any(), any(), any(), any(), any());
    }

    @Test
    void getMyOrders_shouldReturnPagedOrders_whenDefaultParameters() throws Exception {

        OrderResponse order1 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("12345")
                .city("City One")
                .phone("+123456789")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        OrderResponse order2 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("54321")
                .city("City Two")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        List<OrderResponse> content = Arrays.asList(order1, order2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "createdAt");
        Page<OrderResponse> mockPage = new PageImpl<>(content, pageRequest, 2);

        when(orderService.getMyOrders(eq(USER_EMAIL), eq(10), eq(0), eq("ASC"), eq("createdAt"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderId").exists())
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].address").value("Address One"))
                .andExpect(jsonPath("$.content[0].zipCode").value("12345"))
                .andExpect(jsonPath("$.content[0].city").value("City One"))
                .andExpect(jsonPath("$.content[0].phone").value("+123456789"))
                .andExpect(jsonPath("$.content[0].deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.content[0].orderStatus").value(OrderStatus.PAID.name()))

                .andExpect(jsonPath("$.content[1].orderId").exists())
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].address").value("Address Two"))
                .andExpect(jsonPath("$.content[1].zipCode").value("54321"))
                .andExpect(jsonPath("$.content[1].city").value("City Two"))
                .andExpect(jsonPath("$.content[1].phone").value("+987654321"))
                .andExpect(jsonPath("$.content[1].deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.content[1].orderStatus").value(OrderStatus.CREATED.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderService, times(1)).getMyOrders(eq(USER_EMAIL), eq(10), eq(0), eq("ASC"), eq("createdAt"));
    }

    @Test
    void getMyOrders_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getMyOrders(any(), any(), any(), any(), any());
    }

    @Test
    void getMyOrders_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getMyOrders(any(), any(), any(), any(), any());
    }

    @Test
    void getMyOrders_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getMyOrders(any(), any(), any(), any(), any());
    }

    @Test
    void getMyOrders_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        mockMvc.perform(get("/users/me/orders")
                        .with(user(USER_DETAILS))
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be either: 'orderStatus' or 'createdAt'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getMyOrders(any(), any(), any(), any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnUpdatedUser_whenValidRequestAndAuthenticated() throws Exception {

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .userId(USER_ID)
                .email("email@example.com")
                .firstName(updateRequest.getFirstName())
                .lastName(updateRequest.getLastName())
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(userService.updateMyProfile(eq(USER_EMAIL), eq(updateRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value(updateRequest.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(updateRequest.getLastName()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).updateMyProfile(eq(USER_EMAIL), any(UserUpdateRequest.class));
    }


    @Test
    void updateMyProfile_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        mockMvc.perform(patch("/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenFirstNameIsTooShort() throws Exception {

        UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
                .firstName("A")
                .lastName("Updated Last Name")
                .build();

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenFirstNameIsTooLong() throws Exception {

        UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
                .firstName("A".repeat(31))
                .lastName("Updated Last Name")
                .build();

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenLastNameIsTooShort() throws Exception {

        UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("A")
                .build();

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenLastNameIsTooLong() throws Exception {

        UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("A".repeat(31))
                .build();

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    void updateMyProfile_shouldReturnOk_whenOnlyFirstNameIsProvided() throws Exception {

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .userId(USER_ID)
                .firstName(updateRequest.getFirstName())
                .lastName("Original Last Name")
                .email("email@example.com")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(userService.updateMyProfile(eq(USER_EMAIL), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Updated First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).updateMyProfile(eq(USER_EMAIL), eq(updateRequest));
    }

    @Test
    void updateMyProfile_shouldReturnOk_whenOnlyLastNameIsProvided() throws Exception {

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .lastName("Updated Last Name")
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .userId(USER_ID)
                .firstName("Original First Name")
                .lastName(updateRequest.getLastName())
                .email("email@example.com")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(userService.updateMyProfile(eq(USER_EMAIL), eq(updateRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Updated Last Name"))
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).updateMyProfile(eq(USER_EMAIL), eq(updateRequest));
    }

    @Test
    void updateMyProfile_shouldReturnOk_whenEmptyRequestBody() throws Exception {

        UserUpdateRequest updateRequest = UserUpdateRequest.builder().build();

        UserResponse expectedResponse = UserResponse.builder()
                .userId(USER_ID)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .email("email@example.com")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(userService.updateMyProfile(eq(USER_EMAIL), eq(updateRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/me/profile")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).updateMyProfile(eq(USER_EMAIL), eq(updateRequest));
    }

    @Test
    void changeMyPassword_shouldReturnOk_whenUserAuthenticated() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(newPassword)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Password for user with email: %s, has been successfully changed.", USER_EMAIL))
                .build();

        when(userService.changeMyPassword(eq(USER_EMAIL), eq(request))).thenReturn(messageResponse);

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Password for user with email: %s, has been successfully changed.", USER_EMAIL)));

        verify(userService, times(1)).changeMyPassword(eq(USER_EMAIL), eq(request));
    }

    @Test
    void changeMyPassword_shouldReturnUnauthorized_whenUserNotAuthenticated() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(newPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getMyProfile(eq(USER_EMAIL));
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenCurrentMyPasswordIsBlank() throws Exception {

        String currentPassword = "";
        String newPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(newPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Current password is required","Invalid value for 'current password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenCurrentMyPasswordIsNotValid() throws Exception {

        String currentPassword = "Invalid Password";
        String newPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(newPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Invalid value for 'current password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenNewMyPasswordIsBlank() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "";
        String confirmPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(confirmPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "New password is required","Invalid value for 'new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenNewMyPasswordIsNotValid() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "Invalid Password";
        String confirmPassword = "Password123New!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(confirmPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Invalid value for 'new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenConfirmNewMyPasswordIsBlank() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "Password123New!";
        String confirmPassword = "";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(confirmPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Confirm new password field is required","Invalid value for 'confirm new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void changeMyPassword_shouldBadRequest_whenConfirmNewMyPasswordIsNotValid() throws Exception {

        String currentPassword = "Password123!";
        String newPassword = "Password123New!";
        String confirmPassword = "Invalid Password";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmNewPassword(confirmPassword)
                .build();

        mockMvc.perform(patch("/users/me/change-password")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Invalid value for 'confirm new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/users/me/change-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).changeMyPassword(any(), any());
    }

    @Test
    void unregisterMyAccount_shouldReturnOk_whenUserIsAuthenticatedAndRequestIsValid() throws Exception {

        String validPassword = "Password123!";

        UserUnregisterRequest unregisterRequest = UserUnregisterRequest.builder()
                .password(validPassword)
                .confirmPassword(validPassword)
                .build();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("User with email: %s, has been unregistered.", USER_EMAIL))
                .build();

        when(userService.unregisterMyAccount(eq(USER_EMAIL), eq(unregisterRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/me/unregister")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unregisterRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("User with email: %s, has been unregistered.", USER_EMAIL)));

        verify(userService, times(1)).unregisterMyAccount(eq(USER_EMAIL), eq(unregisterRequest));
    }

    @Test
    void unregisterMyAccount_shouldReturnUnauthorized_whenUserIsNotAuthenticated() throws Exception {

        String validPassword = "Password123!";

        UserUnregisterRequest unregisterRequest = UserUnregisterRequest.builder()
                .password(validPassword)
                .confirmPassword(validPassword)
                .build();

        mockMvc.perform(patch("/users/me/unregister")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unregisterRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).unregisterMyAccount(any(), any());
    }

    @Test
    void unregisterMyAccount_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {

        String validPassword = "Password123!";

        UserUnregisterRequest invalidRequest = UserUnregisterRequest.builder()
                .password("")
                .confirmPassword(validPassword)
                .build();

        mockMvc.perform(patch("/users/me/unregister")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Password is required", "Invalid value for 'password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).unregisterMyAccount(any(), any());
    }

    @Test
    void unregisterMyAccount_shouldReturnBadRequest_whenConfirmPasswordIsBlank() throws Exception {

        String validPassword = "Password123!";

        UserUnregisterRequest invalidRequest = UserUnregisterRequest.builder()
                .password(validPassword)
                .confirmPassword("")
                .build();

        mockMvc.perform(patch("/users/me/unregister")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Confirm password field is required", "Invalid value for 'confirm password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).unregisterMyAccount(any(), any());
    }

    @Test
    void unregisterMyAccount_shouldReturnBadRequest_whenPasswordIsNotValid() throws Exception {

        String validPassword = "Password123!";
        String invalidPassword = "INVALID_PASSWORD";

        UserUnregisterRequest invalidRequest = UserUnregisterRequest.builder()
                .password(invalidPassword)
                .confirmPassword(validPassword)
                .build();

        mockMvc.perform(patch("/users/me/unregister")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Invalid value for 'password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).unregisterMyAccount(any(), any());
    }

    @Test
    void unregisterMyAccount_shouldReturnBadRequest_whenConfirmPasswordIsNotValid() throws Exception {

        String validPassword = "Password123!";
        String invalidPassword = "INVALID_PASSWORD";

        UserUnregisterRequest invalidRequest = UserUnregisterRequest.builder()
                .password(validPassword)
                .confirmPassword(invalidPassword)
                .build();

        mockMvc.perform(patch("/users/me/unregister")
                        .with(user(USER_DETAILS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "Invalid value for 'confirm password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).unregisterMyAccount(any(), any());
    }


    // 👮 Admin access endpoints — restricted to users with administrative privileges

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnPagedUsers_whenValidParametersAndAdminRole() throws Exception {

        UserResponse user1 = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email("email1@example.com")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserResponse user2 = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email("email2@example.com")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(UserRole.ADMINISTRATOR)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<UserResponse> content = Arrays.asList(user1, user2);

        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "lastName");
        Page<UserResponse> mockPage = new PageImpl<>(content, pageRequest, 10);

        when(userService.getUsersByStatus(eq(true), eq(false), eq(2), eq(0), eq("DESC"), eq("lastName"))).thenReturn(mockPage);

        mockMvc.perform(get("/users")
                        .param("isEnabled", "true")
                        .param("isNonLocked", "false")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "lastName")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].userId").exists())
                .andExpect(jsonPath("$.content[0].email").value("email1@example.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].isEnabled").value(true))
                .andExpect(jsonPath("$.content[0].isNonLocked").value(false))

                .andExpect(jsonPath("$.content[1].userId").exists())
                .andExpect(jsonPath("$.content[1].email").value("email2@example.com"))
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].isEnabled").value(true))
                .andExpect(jsonPath("$.content[1].isNonLocked").value(false))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(5));

        verify(userService, times(1)).getUsersByStatus(eq(true), eq(false), eq(2), eq(0), eq("DESC"), eq("lastName"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getUsersByStatus_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/users")
                        .param("isEnabled", "true")
                        .param("isNonLocked", "false")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "lastName")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getUsersByStatus_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/users")
                        .param("isEnabled", "true")
                        .param("isNonLocked", "false")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "lastName")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnPagedUsers_whenDefaultParameters() throws Exception {

        UserResponse user1 = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email("email1@example.com")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        UserResponse user2 = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email("email2@example.com")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(UserRole.ADMINISTRATOR)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<UserResponse> content = Arrays.asList(user1, user2);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "registeredAt");
        Page<UserResponse> mockPage = new PageImpl<>(content, pageRequest, 20);

        when(userService.getUsersByStatus(eq(true), eq(true), eq(10), eq(0), eq("ASC"), eq("registeredAt"))).thenReturn(mockPage);

        mockMvc.perform(get("/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].userId").exists())
                .andExpect(jsonPath("$.content[0].email").value("email1@example.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].isEnabled").value(true))
                .andExpect(jsonPath("$.content[0].isNonLocked").value(true))

                .andExpect(jsonPath("$.content[1].userId").exists())
                .andExpect(jsonPath("$.content[1].email").value("email2@example.com"))
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].isEnabled").value(true))
                .andExpect(jsonPath("$.content[1].isNonLocked").value(true))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(userService, times(1)).getUsersByStatus(eq(true), eq(true), eq(10), eq(0), eq("ASC"), eq("registeredAt"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidBooleanFormatForIsEnabled() throws Exception {

        mockMvc.perform(get("/users")
                        .param("isEnabled", "INVALID_FORMAT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.details").value("Invalid value 'INVALID_FORMAT' for parameter 'isEnabled'. Expected type: Boolean"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidBooleanFormatForIsNonLocked() throws Exception {

        mockMvc.perform(get("/users")
                        .param("isNonLocked", "INVALID_FORMAT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.details").value("Invalid value 'INVALID_FORMAT' for parameter 'isNonLocked'. Expected type: Boolean"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/users")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/users")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/users")
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUsersByStatus_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        mockMvc.perform(get("/users")
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be one of the following: 'firstName', 'lastName', 'registeredAt', 'updatedAt'")))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUsersByStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserById_shouldReturnUser_whenValidIdAndAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        UserResponse expectedUser = UserResponse.builder()
                .userId(UUID.fromString(validUserId))
                .email("email@example.com")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        when(userService.getUserById(eq(validUserId))).thenReturn(expectedUser);

        mockMvc.perform(get("/users/{userId}", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(validUserId))
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.firstName").value("First Name"))
                .andExpect(jsonPath("$.lastName").value("Last Name"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.registeredAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService, times(1)).getUserById(eq(validUserId));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getUserById_shouldReturnUnauthorized_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUserById(any());
    }

    @Test
    void getUserById_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUserById(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserById_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(get("/users/{userId}", invalidUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format in path variable")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).getUserById(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnPagedWishlistItems_whenValidParametersAndAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        WishListItemResponse item1 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .product(product1)
                .build();

        WishListItemResponse item2 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<WishListItemResponse> content = Arrays.asList(item1, item2);

        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "addedAt");
        Page<WishListItemResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(wishListService.getUserWishListItems(eq(validUserId), eq(2), eq(0), eq("DESC"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].wishListItemId").exists())
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].wishListItemId").exists())
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(wishListService, times(1)).getUserWishListItems(eq(validUserId), eq(2), eq(0), eq("DESC"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getUserWishListItems_shouldReturnUnauthorized_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    void getUserWishListItems_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnPagedWishlistItems_whenDefaultParameters() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        WishListItemResponse item1 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .product(product1)
                .build();

        WishListItemResponse item2 = WishListItemResponse.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<WishListItemResponse> content = Arrays.asList(item1, item2);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "addedAt");
        Page<WishListItemResponse> mockPage = new PageImpl<>(content, pageRequest, 10);

        when(wishListService.getUserWishListItems(eq(validUserId), eq(10), eq(0), eq("ASC")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].wishListItemId").exists())
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].wishListItemId").exists())
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(wishListService, times(1)).getUserWishListItems(eq(validUserId), eq(10), eq(0), eq("ASC"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(get("/users/{userId}/wishListItems", invalidUserId)
                        .param("size", "10")
                        .param("page", "0")
                        .param("order", "ASC")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserWishListItems_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/wishListItems", validUserId)
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(wishListService, never()).getUserWishListItems(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnPagedCartItems_whenValidParametersAndAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse item1 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .product(product1)
                .build();

        CartItemResponse item2 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<CartItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "quantity");
        Page<CartItemResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(cartService.getUserCartItems(eq(validUserId), eq(2), eq(0), eq("DESC"), eq("quantity"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "quantity")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].cartItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].cartItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(1))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(cartService, times(1)).getUserCartItems(eq(validUserId), eq(2), eq(0), eq("DESC"), eq("quantity"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getUserCartItems_shouldReturnUnauthorized_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    void getUserCartItems_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnPagedCartItems_whenDefaultParameters() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        ProductResponse product1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse product2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name Two")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        CartItemResponse item1 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .product(product1)
                .build();

        CartItemResponse item2 = CartItemResponse.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .product(product2)
                .build();

        List<CartItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "addedAt");
        Page<CartItemResponse> mockPage = new PageImpl<>(content, pageRequest, 2);

        when(cartService.getUserCartItems(eq(validUserId), eq(10), eq(0), eq("ASC"), eq("addedAt")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].cartItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product Name One"))

                .andExpect(jsonPath("$.content[1].cartItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(1))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Name Two"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(cartService, times(1)).getUserCartItems(eq(validUserId), eq(10), eq(0), eq("ASC"), eq("addedAt"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(get("/users/{userId}/cartItems", invalidUserId)
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserCartItems_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/cartItems", validUserId)
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be either: 'addedAt' or 'quantity'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cartService, never()).getUserCartItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnPagedOrders_whenValidParametersAndAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderResponse order1 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("12345")
                .city("City One")
                .phone("+123456789")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        OrderResponse order2 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("54321")
                .city("City Two")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        List<OrderResponse> content = Arrays.asList(order1, order2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "status");
        Page<OrderResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(orderService.getUserOrders(eq(validUserId), eq(2), eq(0), eq("DESC"), eq("status"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "status")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderId").exists())
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].address").value("Address One"))
                .andExpect(jsonPath("$.content[0].zipCode").value("12345"))
                .andExpect(jsonPath("$.content[0].city").value("City One"))
                .andExpect(jsonPath("$.content[0].phone").value("+123456789"))
                .andExpect(jsonPath("$.content[0].deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.content[0].orderStatus").value(OrderStatus.PAID.name()))

                .andExpect(jsonPath("$.content[1].orderId").exists())
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].address").value("Address Two"))
                .andExpect(jsonPath("$.content[1].zipCode").value("54321"))
                .andExpect(jsonPath("$.content[1].city").value("City Two"))
                .andExpect(jsonPath("$.content[1].phone").value("+987654321"))
                .andExpect(jsonPath("$.content[1].deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.content[1].orderStatus").value(OrderStatus.CREATED.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orderService, times(1)).getUserOrders(eq(validUserId), eq(2), eq(0), eq("DESC"), eq("status"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getUserOrders_shouldReturnUnauthorized_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    void getUserOrders_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnPagedOrders_whenDefaultParameters() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderResponse order1 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("12345")
                .city("City One")
                .phone("+123456789")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        OrderResponse order2 = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("54321")
                .city("City Two")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        List<OrderResponse> content = Arrays.asList(order1, order2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "createdAt");
        Page<OrderResponse> mockPage = new PageImpl<>(content, pageRequest, 2);

        when(orderService.getUserOrders(eq(validUserId), eq(10), eq(0), eq("ASC"), eq("createdAt"))).thenReturn(mockPage);

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderId").exists())
                .andExpect(jsonPath("$.content[0].firstName").value("First Name One"))
                .andExpect(jsonPath("$.content[0].lastName").value("Last Name One"))
                .andExpect(jsonPath("$.content[0].address").value("Address One"))
                .andExpect(jsonPath("$.content[0].zipCode").value("12345"))
                .andExpect(jsonPath("$.content[0].city").value("City One"))
                .andExpect(jsonPath("$.content[0].phone").value("+123456789"))
                .andExpect(jsonPath("$.content[0].deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.content[0].orderStatus").value(OrderStatus.PAID.name()))

                .andExpect(jsonPath("$.content[1].orderId").exists())
                .andExpect(jsonPath("$.content[1].firstName").value("First Name Two"))
                .andExpect(jsonPath("$.content[1].lastName").value("Last Name Two"))
                .andExpect(jsonPath("$.content[1].address").value("Address Two"))
                .andExpect(jsonPath("$.content[1].zipCode").value("54321"))
                .andExpect(jsonPath("$.content[1].city").value("City Two"))
                .andExpect(jsonPath("$.content[1].phone").value("+987654321"))
                .andExpect(jsonPath("$.content[1].deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.content[1].orderStatus").value(OrderStatus.CREATED.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderService, times(1)).getUserOrders(eq(validUserId), eq(10), eq(0), eq("ASC"), eq("createdAt"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(get("/users/{userId}/orders", invalidUserId)
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getUserOrders_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users/{userId}/orders", validUserId)
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be either: 'orderStatus' or 'createdAt'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getUserOrders(any(), any(), any(), any(), any());
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setUserRole_shouldReturnOk_whenValidParametersAndAdministratorRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        String targetRole = UserRole.ADMINISTRATOR.name();

        MessageResponse expectedResponse = new MessageResponse(String.format("UserRole %s was set for user with id: %s.", targetRole, validUserId));

        when(userService.setUserRole(eq(validUserId), eq(targetRole))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/{userId}/role", validUserId)
                        .param("role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("UserRole %s was set for user with id: %s.", targetRole, validUserId)));

        verify(userService, times(1)).setUserRole(eq(validUserId), eq(targetRole));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void setUserRole_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/users/{userId}/role", validUserId)
                        .param("role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).setUserRole(any(), any());
    }

    @Test
    void setUserRole_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/users/{userId}/role", validUserId)
                        .param("role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).setUserRole(any(), any());
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setUserRole_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(patch("/users/{userId}/role", invalidUserId)
                        .param("role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).setUserRole(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setUserRole_shouldReturnBadRequest_whenInvalidRoleValue() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/users/{userId}/role", validUserId)
                        .param("role", "SUPER_ADMIN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order orderStatus: Must be one of the: 'CLIENT' or 'ADMINISTRATOR' ('client' or 'administrator')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).setUserRole(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void toggleUserLockState_shouldReturnOk_whenUserIsLocked() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been unlocked.", validUserId))
                .build();

        when(userService.toggleUserLockState(eq(validUserId))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/{userId}/toggle-lock", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("User with id: %s has been unlocked.", validUserId)));

        verify(userService, times(1)).toggleUserLockState(eq(validUserId));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void toggleUserLockState_shouldReturnOk_whenUserIsUnlocked() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been locked.", validUserId))
                .build();

        when(userService.toggleUserLockState(eq(validUserId))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/users/{userId}/toggle-lock", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("User with id: %s has been locked.", validUserId)));

        verify(userService, times(1)).toggleUserLockState(eq(validUserId));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void toggleUserLockState_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/users/{userId}/toggle-lock", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).toggleUserLockState(any());
    }

    @Test
    void toggleUserLockState_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/users/{userId}/toggle-lock", validUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).toggleUserLockState(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void toggleUserLockState_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        mockMvc.perform(patch("/users/{userId}/toggle-lock", invalidUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).toggleUserLockState(any());
    }
}