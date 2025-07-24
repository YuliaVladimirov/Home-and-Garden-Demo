package org.example.homeandgarden.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.order.dto.OrderCreateRequest;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.dto.OrderUpdateRequest;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.order.service.OrderItemServiceImpl;
import org.example.homeandgarden.order.service.OrderServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OrderControllerTest.TestConfig.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public OrderServiceImpl orderService() {
            return mock(OrderServiceImpl.class);
        }

        @Bean
        @Primary
        public OrderItemServiceImpl orderItemService() {
            return mock(OrderItemServiceImpl.class);
        }

    }

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrderItemServiceImpl orderItemService;

    @AfterEach
    void resetMocks() {
        reset(orderService, orderItemService);
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnPagedOrderItems_whenValidParametersAndAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        OrderItemResponse item1 = OrderItemResponse.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(40.00))
                .product(productResponse1)
                .build();

        OrderItemResponse item2 = OrderItemResponse.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(50.00))
                .product(productResponse2)
                .build();

        List<OrderItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.ASC, "quantity");
        Page<OrderItemResponse> mockPage = new PageImpl<>(content, pageRequest, 5);

        when(orderItemService.getOrderItems(eq(validOrderId), eq(2), eq(0), eq("DESC"), eq("quantity"))).thenReturn(mockPage);

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "quantity")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(1))
                .andExpect(jsonPath("$.content[0].priceAtPurchase").value(BigDecimal.valueOf(40.00)))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].product.productStatus").value(ProductStatus.AVAILABLE.name()))

                .andExpect(jsonPath("$.content[1].orderItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(2))
                .andExpect(jsonPath("$.content[1].priceAtPurchase").value(BigDecimal.valueOf(50.00)))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].product.productStatus").value(ProductStatus.OUT_OF_STOCK.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orderItemService, times(1)).getOrderItems(eq(validOrderId), eq(2), eq(0), eq("DESC"), eq("quantity"));
    }

    @Test
    void getOrderItems_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnPagedOrderItems_whenDefaultParameters() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .productStatus(ProductStatus.AVAILABLE)
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .productStatus(ProductStatus.OUT_OF_STOCK)
                .build();

        OrderItemResponse item1 = OrderItemResponse.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(40.00))
                .product(productResponse1)
                .build();

        OrderItemResponse item2 = OrderItemResponse.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(50.00))
                .product(productResponse2)
                .build();


        List<OrderItemResponse> content = Arrays.asList(item1, item2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "priceAtPurchase");
        Page<OrderItemResponse> mockPage = new PageImpl<>(content, pageRequest, 10);

        when(orderItemService.getOrderItems(eq(validOrderId), eq(10), eq(0), eq("ASC"), eq("priceAtPurchase")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].orderItemId").exists())
                .andExpect(jsonPath("$.content[0].quantity").value(1))
                .andExpect(jsonPath("$.content[0].priceAtPurchase").value(BigDecimal.valueOf(40.00)))
                .andExpect(jsonPath("$.content[0].product.productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].product.productStatus").value(ProductStatus.AVAILABLE.name()))

                .andExpect(jsonPath("$.content[1].orderItemId").exists())
                .andExpect(jsonPath("$.content[1].quantity").value(2))
                .andExpect(jsonPath("$.content[1].priceAtPurchase").value(BigDecimal.valueOf(50.00)))
                .andExpect(jsonPath("$.content[1].product.productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].product.productStatus").value(ProductStatus.OUT_OF_STOCK.name()))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderItemService, times(1)).getOrderItems(eq(validOrderId), eq(10), eq(0), eq("ASC"), eq("priceAtPurchase"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnBadRequest_whenInvalidOrderIdFormat() throws Exception {

        String invalidOrderId = "INVALID_UUID";

        mockMvc.perform(get("/orders/{orderId}/items", invalidOrderId)
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "quantity")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderItems_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/items", validOrderId)
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be either: 'quantity' or 'priceAtPurchase'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderItemService, never()).getOrderItems(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderById_shouldReturnOrder_whenValidIdAndAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderResponse expectedOrder = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+123456789")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        when(orderService.getOrderById(eq(validOrderId))).thenReturn(expectedOrder);

        mockMvc.perform(get("/orders/{orderId}", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("First Name"))
                .andExpect(jsonPath("$.lastName").value("Last Name"))
                .andExpect(jsonPath("$.address").value("Address"))
                .andExpect(jsonPath("$.zipCode").value("12345"))
                .andExpect(jsonPath("$.city").value("City"))
                .andExpect(jsonPath("$.phone").value("+123456789"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PAID.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).getOrderById(eq(validOrderId));
    }

    @Test
    void getOrderById_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getOrderById(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderById_shouldReturnBadRequest_whenInvalidOrderIdFormat() throws Exception {

        String invalidOrderId = "INVALID_UUID";

        mockMvc.perform(get("/orders/{orderId}", invalidOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getOrderById(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderStatus_shouldReturnOk_whenValidIdAndAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();
        OrderStatus status = OrderStatus.PAID;

        MessageResponse expectedResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s has status '%s'.", validOrderId, status.name()))
                .build();

        when(orderService.getOrderStatus(eq(validOrderId))).thenReturn(expectedResponse);

        mockMvc.perform(get("/orders/{orderId}/status", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(String.format("Order with id: %s has status '%s'.", validOrderId, status.name())));

        verify(orderService, times(1)).getOrderStatus(eq(validOrderId));
    }

    @Test
    void getOrderStatus_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{orderId}/status", validOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getOrderStatus(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getOrderStatus_shouldReturnBadRequest_whenInvalidOrderIdFormat() throws Exception {

        String invalidOrderId = "INVALID_UUID";

        mockMvc.perform(get("/orders/{orderId}/status", invalidOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).getOrderStatus(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnCreatedOrder_whenValidRequestAndClientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest createRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .firstName(createRequest.getFirstName())
                .lastName(createRequest.getLastName())
                .address(createRequest.getAddress())
                .zipCode(createRequest.getZipCode())
                .city(createRequest.getCity())
                .phone(createRequest.getPhone())
                .deliveryMethod(DeliveryMethod.valueOf(createRequest.getDeliveryMethod()))
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(orderService.addOrder(eq(createRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.firstName").value("First Name"))
                .andExpect(jsonPath("$.lastName").value("Last Name"))
                .andExpect(jsonPath("$.address").value("Address"))
                .andExpect(jsonPath("$.zipCode").value("12345"))
                .andExpect(jsonPath("$.city").value("City"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).addOrder(eq(createRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addOrder_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest createRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    void addOrder_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest createRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenInvalidUserIdFormat() throws Exception {

        String invalidUserId = "INVALID_UUID";

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(invalidUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenUserIdIsBlank() throws Exception {

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId("")
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("User id is required", "Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenFirstNameIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("First name is required",
                        "Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenFirstNameIsTooShort() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("A")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenFirstNameIsTooLong() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("A".repeat(31))
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenLastNameIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Last name is required",
                        "Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenLastNameIsTooShort() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("A")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenLastNameIsTooLong() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("A".repeat(31))
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenAddressIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();
        
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Address is required")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenAddressIsTooLong() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("A".repeat(256))
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Address must be at most 255 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenZipCodeIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();


        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Zip code is required",
                        "ZIP code must be exactly 5 digits")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenZipCodeIsInvalidFormat() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("123")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("ZIP code must be exactly 5 digits")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());
    
        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenCityIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("City is required",
                        "Invalid city: Must be of 2 - 100 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenCityIsTooShort() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("A")
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid city: Must be of 2 - 100 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenCityIsTooLong() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("A".repeat(101))
                .phone("+1234567890")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid city: Must be of 2 - 100 characters")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenPhoneIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Phone is required",
                        "Invalid phone number: Must be of 9 - 15 digits")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenPhoneIsInvalidFormat() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("123")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY.name())
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid phone number: Must be of 9 - 15 digits")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenDeliveryMethodIsBlank() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod("")
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Delivery method is required",
                        "Invalid Delivery method: Must be one of: 'COURIER_DELIVERY' or 'CUSTOMER_PICKUP'")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addOrder_shouldReturnBadRequest_whenDeliveryMethodIsInvalid() throws Exception {

        String validUserId = UUID.randomUUID().toString();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(validUserId)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("12345")
                .city("City")
                .phone("+1234567890")
                .deliveryMethod("INVALID_DELIVERY_METHOD")
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid Delivery method: Must be one of: 'COURIER_DELIVERY' or 'CUSTOMER_PICKUP'")))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).addOrder(any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnUpdatedOrder_whenValidRequestAndClientRole() throws Exception {
        
        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName(updateRequest.getFirstName())
                .lastName(updateRequest.getLastName())
                .address(updateRequest.getAddress())
                .zipCode(updateRequest.getZipCode())
                .city(updateRequest.getCity())
                .phone(updateRequest.getPhone())
                .deliveryMethod(DeliveryMethod.valueOf(updateRequest.getDeliveryMethod()))
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Updated First Name"))
                .andExpect(jsonPath("$.lastName").value("Updated Last Name"))
                .andExpect(jsonPath("$.address").value("Updated Address"))
                .andExpect(jsonPath("$.zipCode").value("54321"))
                .andExpect(jsonPath("$.city").value("Updated City"))
                .andExpect(jsonPath("$.phone").value("+987654321"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateOrder_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    void updateOrder_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }


    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenInvalidOrderIdFormat() throws Exception {

        String invalidOrderId = "INVALID_UUID";

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", invalidOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenFirstNameIsTooShort() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("A")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenFirstNameIsTooLong() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("A".repeat(31))
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenLastNameIsTooShort() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("A")
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenLastNameIsTooLong() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("A".repeat(31))
                .address("Updated Address")
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenAddressIsTooLong() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("A".repeat(256))
                .zipCode("54321")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Delivery address must be less than or equal to 255 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenZipCodeIsInvalidFormat() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("1")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("ZIP code must be exactly 5 digits")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenCityIsTooShort() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("12345")
                .city("A")
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid city: Must be of 2 - 100 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenCityIsTooLong() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("12345")
                .city("A".repeat(101))
                .phone("+987654321")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid city: Must be of 2 - 100 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenPhoneIsInvalidFormat() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("12345")
                .city("Updated City")
                .phone("1")
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid phone number: Must be of 9 - 15 digits")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnBadRequest_whenDeliveryMethodIsInvalid() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest invalidRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("12345")
                .city("Updated City")
                .phone("+987654321")
                .deliveryMethod("INVALID_DELIVERY_METHOD")
                .build();

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid Delivery method: Must be one of: 'COURIER_DELIVERY' or 'CUSTOMER_PICKUP'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).updateOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenEmptyRequestBody() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder().build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyFirstNameIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName(updateRequest.getFirstName())
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Updated First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyLastNameIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .lastName("Updated Last Name")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName(updateRequest.getLastName())
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Updated Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyAddressIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .address("Updated Address")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address(updateRequest.getAddress())
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Updated Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyZipCodeIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .zipCode("12345")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode(updateRequest.getZipCode())
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("12345"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyCityIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .city("Updated City")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city(updateRequest.getCity())
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Updated City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyPhoneIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .phone("+123456789")
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone(updateRequest.getPhone())
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("+123456789"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.COURIER_DELIVERY.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateOrder_shouldReturnOk_whenOnlyDeliveryMethodIsProvided() throws Exception {

        String validOrderId = UUID.randomUUID().toString();

        OrderUpdateRequest updateRequest = OrderUpdateRequest.builder()
                .deliveryMethod(DeliveryMethod.CUSTOMER_PICKUP.name())
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .orderId(UUID.fromString(validOrderId))
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("Original Phone")
                .deliveryMethod(DeliveryMethod.valueOf(updateRequest.getDeliveryMethod()))
                .orderStatus(OrderStatus.CREATED)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateOrder(eq(validOrderId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/orders/{orderId}", validOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(validOrderId))
                .andExpect(jsonPath("$.firstName").value("Original First Name"))
                .andExpect(jsonPath("$.lastName").value("Original Last Name"))
                .andExpect(jsonPath("$.address").value("Original Address"))
                .andExpect(jsonPath("$.zipCode").value("Original Zip Code"))
                .andExpect(jsonPath("$.city").value("Original City"))
                .andExpect(jsonPath("$.phone").value("Original Phone"))
                .andExpect(jsonPath("$.deliveryMethod").value(DeliveryMethod.CUSTOMER_PICKUP.name()))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService, times(1)).updateOrder(eq(validOrderId), eq(updateRequest));
    }
}