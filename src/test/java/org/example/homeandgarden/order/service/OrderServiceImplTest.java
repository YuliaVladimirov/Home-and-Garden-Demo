package org.example.homeandgarden.order.service;

import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.cart.repository.CartRepository;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.dto.OrderCreateRequest;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.dto.OrderUpdateRequest;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.order.mapper.OrderItemMapper;
import org.example.homeandgarden.order.mapper.OrderMapper;
import org.example.homeandgarden.order.repository.OrderItemRepository;
import org.example.homeandgarden.order.repository.OrderRepository;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 5;
    private static final String ORDER = "ASC";
    private static final String SORT_BY = "createdAt";

    private static final UUID ORDER_ID = UUID.fromString("7e2b1a9f-4d8c-4f3a-a2b1-9f6e7d5c3a1b");
    private static final UUID NON_EXISTING_ORDER_ID = UUID.fromString("b3f9a2e1-c6d4-49f0-9a7e-1e5d4c3b2a8f");

    private static final UUID USER_ID = UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76");
    private static final UUID NON_EXISTING_USER_ID = UUID.fromString("28123116-33f4-4b89-8519-b22ac2350834");

    private static final String INVALID_ID = "Invalid UUID";

    private static final String USER_EMAIL = "user@example.com";
    private static final String NON_EXISTING_USER_EMAIL = "nonExistingUser@example.com";

    private static final DeliveryMethod COURIER_DELIVERY = DeliveryMethod.COURIER_DELIVERY;
    private static final DeliveryMethod CUSTOMER_PICKUP = DeliveryMethod.CUSTOMER_PICKUP;

    private static final OrderStatus ORDER_STATUS_CREATED = OrderStatus.CREATED;
    private static final OrderStatus ORDER_STATUS_PAID = OrderStatus.PAID;
    private static final OrderStatus ORDER_STATUS_ON_THE_WAY = OrderStatus.ON_THE_WAY;
    private static final OrderStatus ORDER_STATUS_DELIVERED = OrderStatus.DELIVERED;
    private static final OrderStatus ORDER_STATUS_RETURNED = OrderStatus.RETURNED;
    private static final OrderStatus ORDER_STATUS_CANCELED = OrderStatus.CANCELED;

    private final Instant TIMESTAMP_NOW = Instant.now();
    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

    @Test
    void getUserOrders_shouldReturnPagedOrdersWhenUserExists() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Order order1 = Order.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order order2 = Order.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("Zip Code Two")
                .city("City Two")
                .phone("456")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        List<Order> orders = List.of(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orders, pageRequest, orders.size());
        long expectedTotalPages = (long) Math.ceil((double) orders.size() / SIZE);

        OrderResponse orderResponse1 = OrderResponse.builder()
                .orderId(order1.getOrderId())
                .firstName(order1.getFirstName())
                .lastName(order1.getLastName())
                .address(order1.getAddress())
                .zipCode(order1.getZipCode())
                .city(order1.getCity())
                .phone(order1.getPhone())
                .deliveryMethod(order1.getDeliveryMethod())
                .orderStatus(order1.getOrderStatus())
                .createdAt(order1.getCreatedAt())
                .updatedAt(order1.getUpdatedAt())
                .build();

        OrderResponse orderResponse2 = OrderResponse.builder()
                .orderId(order2.getOrderId())
                .firstName(order2.getFirstName())
                .lastName(order2.getLastName())
                .address(order2.getAddress())
                .zipCode(order2.getZipCode())
                .city(order2.getCity())
                .phone(order2.getPhone())
                .deliveryMethod(order2.getDeliveryMethod())
                .orderStatus(order2.getOrderStatus())
                .createdAt(order2.getCreatedAt())
                .updatedAt(order2.getUpdatedAt())
                .build();

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(orderRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(orderPage);
        when(orderMapper.orderToResponse(order1)).thenReturn(orderResponse1);
        when(orderMapper.orderToResponse(order2)).thenReturn(orderResponse2);

        Page<OrderResponse> actualResponse = orderService.getUserOrders(USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(orderRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(orderMapper, times(1)).orderToResponse(order1);
        verify(orderMapper, times(1)).orderToResponse(order2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(orders.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(orderResponse1.getOrderId(), actualResponse.getContent().getFirst().getOrderId());
        assertEquals(orderResponse2.getOrderId(), actualResponse.getContent().get(1).getOrderId());
    }

    @Test
    void getUserOrders_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                orderService.getUserOrders(INVALID_ID, SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, never()).existsByUserId(any(UUID.class));
        verify(orderRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));
    }

    @Test
    void getUserOrders_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.existsByUserId(NON_EXISTING_USER_ID)).thenReturn(false);

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderService.getUserOrders(NON_EXISTING_USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, times(1)).existsByUserId(NON_EXISTING_USER_ID);
        verify(orderRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void getUserOrders_shouldReturnEmptyPagedModelUserHasNoOrders() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Page<Order> emptyOrderPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(orderRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(emptyOrderPage);

        Page<OrderResponse> actualResponse = orderService.getUserOrders(USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(orderRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getMyOrders_shouldReturnPagedOrdersWhenUserExists() {

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(USER_EMAIL)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Order order1 = Order.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order order2 = Order.builder()
                .orderId(UUID.randomUUID())
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .address("Address Two")
                .zipCode("Zip Code Two")
                .city("City Two")
                .phone("456")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        List<Order> orders = List.of(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orders, pageRequest, orders.size());
        long expectedTotalPages = (long) Math.ceil((double) orders.size() / SIZE);

        OrderResponse orderResponse1 = OrderResponse.builder()
                .orderId(order1.getOrderId())
                .firstName(order1.getFirstName())
                .lastName(order1.getLastName())
                .address(order1.getAddress())
                .zipCode(order1.getZipCode())
                .city(order1.getCity())
                .phone(order1.getPhone())
                .deliveryMethod(order1.getDeliveryMethod())
                .orderStatus(order1.getOrderStatus())
                .createdAt(order1.getCreatedAt())
                .updatedAt(order1.getUpdatedAt())
                .build();

        OrderResponse orderResponse2 = OrderResponse.builder()
                .orderId(order2.getOrderId())
                .firstName(order2.getFirstName())
                .lastName(order2.getLastName())
                .address(order2.getAddress())
                .zipCode(order2.getZipCode())
                .city(order2.getCity())
                .phone(order2.getPhone())
                .deliveryMethod(order2.getDeliveryMethod())
                .orderStatus(order2.getOrderStatus())
                .createdAt(order2.getCreatedAt())
                .updatedAt(order2.getUpdatedAt())
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(orderRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(orderPage);
        when(orderMapper.orderToResponse(order1)).thenReturn(orderResponse1);
        when(orderMapper.orderToResponse(order2)).thenReturn(orderResponse2);

        Page<OrderResponse> actualResponse = orderService.getMyOrders(USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(orderRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(orderMapper, times(1)).orderToResponse(order1);
        verify(orderMapper, times(1)).orderToResponse(order2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(orders.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(orderResponse1.getOrderId(), actualResponse.getContent().getFirst().getOrderId());
        assertEquals(orderResponse2.getOrderId(), actualResponse.getContent().get(1).getOrderId());
    }

    @Test
    void getMyOrders_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderService.getMyOrders(NON_EXISTING_USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(orderRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void getMyOrders_shouldReturnEmptyPagedModelUserHasNoOrders() {

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(USER_EMAIL)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Page<Order> emptyOrderPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(orderRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(emptyOrderPage);

        Page<OrderResponse> actualResponse = orderService.getMyOrders(USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(orderRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getOrderById_shouldReturnOrderResponseWhenOrderExists() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(existingOrder.getOrderStatus())
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(existingOrder.getUpdatedAt())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderMapper.orderToResponse(existingOrder)).thenReturn(orderResponse);

        OrderResponse actualResponse = orderService.getOrderById(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderMapper, times(1)).orderToResponse(existingOrder);

        assertEquals(orderResponse.getOrderId(), actualResponse.getOrderId());
        assertEquals(orderResponse.getOrderStatus(), actualResponse.getOrderStatus());
        assertEquals(orderResponse.getDeliveryMethod(), actualResponse.getDeliveryMethod());
    }

    @Test
    void getOrderById_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderById(INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));
    }

    @Test
    void getOrderById_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.getOrderById(NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }




    @Test
    void getMyOrderById_shouldReturnOrderResponseWhenOrderExists() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(existingOrder.getOrderStatus())
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(existingOrder.getUpdatedAt())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderMapper.orderToResponse(existingOrder)).thenReturn(orderResponse);

        OrderResponse actualResponse = orderService.getMyOrderById(USER_EMAIL, ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderMapper, times(1)).orderToResponse(existingOrder);

        assertEquals(orderResponse.getOrderId(), actualResponse.getOrderId());
        assertEquals(orderResponse.getOrderStatus(), actualResponse.getOrderStatus());
        assertEquals(orderResponse.getDeliveryMethod(), actualResponse.getDeliveryMethod());
    }

    @Test
    void getMyOrderById_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.getMyOrderById(USER_EMAIL, INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));
    }

    @Test
    void getMyOrderById_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.getMyOrderById(USER_EMAIL, NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void getMyOrderById_shouldReturnAccessDeniedWhenOrderDoesNotBelongToUser() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email("anotherUser@example.com").build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () ->
                orderService.getMyOrderById(USER_EMAIL, ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("Order with id: %s, does not belong to the user with email: %s.", ORDER_ID, USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void getOrderStatus_shouldReturnMessageResponseWhenOrderExists() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s has status '%s'.", ORDER_ID, existingOrder.getOrderStatus().name()))
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        MessageResponse actualResponse = orderService.getOrderStatus(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void getOrderStatus_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderStatus(INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
    }

    @Test
    void getOrderStatus_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.getOrderStatus(NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void getMyOrderStatus_shouldReturnMessageResponseWhenOrderExists() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s has status '%s'.", ORDER_ID, existingOrder.getOrderStatus().name()))
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        MessageResponse actualResponse = orderService.getMyOrderStatus(USER_EMAIL, ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void getMyOrderStatus_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.getMyOrderStatus(USER_EMAIL, INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
    }

    @Test
    void getMyOrderStatus_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.getMyOrderStatus(USER_EMAIL, NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void getMyOrderStatus_shouldReturnAccessDeniedWhenOrderDoesNotBelongToUser() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email("anotherUser@example.com").build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () ->
                orderService.getMyOrderStatus(USER_EMAIL, ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(ORDER_ID);

        assertEquals(String.format("Order with id: %s, does not belong to the user with email: %s.", ORDER_ID, USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void addOrder_shouldAddOrderSuccessfully() {

        OrderCreateRequest orderCreateRequest =  OrderCreateRequest.builder()
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY.name())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product existingInCartProduct = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(3)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(existingUser)
                .product(existingInCartProduct)
                .build();

        existingUser.setCart(Set.of(existingCartItem));

        Order orderToAdd = Order.builder()
                .orderId(null)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CREATED)
                .createdAt(null)
                .updatedAt(null)
                .user(existingUser)
                .build();

        Order addedOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName(orderToAdd.getFirstName())
                .lastName(orderToAdd.getLastName())
                .address(orderToAdd.getAddress())
                .zipCode(orderToAdd.getZipCode())
                .city(orderToAdd.getCity())
                .phone(orderToAdd.getPhone())
                .deliveryMethod(orderToAdd.getDeliveryMethod())
                .orderStatus(orderToAdd.getOrderStatus())
                .createdAt(TIMESTAMP_NOW)
                .updatedAt(TIMESTAMP_NOW)
                .user(orderToAdd.getUser())
                .build();

        OrderItem orderItemToAdd = OrderItem.builder()
                .orderItemId(null)
                .quantity(existingCartItem.getQuantity())
                .priceAtPurchase(existingInCartProduct.getCurrentPrice())
                .order(addedOrder)
                .product(existingInCartProduct)
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(addedOrder.getOrderId())
                .firstName(addedOrder.getFirstName())
                .lastName(addedOrder.getLastName())
                .address(addedOrder.getAddress())
                .zipCode(addedOrder.getZipCode())
                .city(addedOrder.getCity())
                .phone(addedOrder.getPhone())
                .deliveryMethod(addedOrder.getDeliveryMethod())
                .orderStatus(addedOrder.getOrderStatus())
                .createdAt(addedOrder.getCreatedAt())
                .updatedAt(addedOrder.getUpdatedAt())
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderItem> orderItemCaptor = ArgumentCaptor.forClass(OrderItem.class);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(orderMapper.orderRequestToOrder(orderCreateRequest, existingUser)).thenReturn(orderToAdd);
        when(orderRepository.saveAndFlush(orderToAdd)).thenReturn(addedOrder);
        when(orderItemMapper.cartItemToOrderItem(existingCartItem, addedOrder, existingCartItem.getProduct())).thenReturn(orderItemToAdd);
        when(orderMapper.orderToResponse(addedOrder)).thenReturn(orderResponse);

        OrderResponse actualResponse = orderService.addOrder(USER_EMAIL, orderCreateRequest);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(orderMapper, times(1)).orderRequestToOrder(orderCreateRequest, existingUser);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingUser, capturedOrder.getUser());
        assertEquals(ORDER_STATUS_CREATED, capturedOrder.getOrderStatus());
        assertEquals(COURIER_DELIVERY, capturedOrder.getDeliveryMethod());

        verify(orderItemMapper, times(1)).cartItemToOrderItem(existingCartItem, addedOrder, existingCartItem.getProduct());

        verify(orderItemRepository, times(1)).saveAndFlush(orderItemCaptor.capture());
        OrderItem capturedOrderItem = orderItemCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(addedOrder, capturedOrderItem.getOrder());
        assertEquals(existingInCartProduct, capturedOrderItem.getProduct());

        verify(cartRepository, times(1)).deleteAll(Set.of(existingCartItem));
        verify(orderMapper, times(1)).orderToResponse(addedOrder);

        assertNotNull(actualResponse);
        assertEquals(orderResponse.getOrderId(), actualResponse.getOrderId());
        assertEquals(orderResponse.getOrderStatus(), actualResponse.getOrderStatus());
        assertEquals(orderResponse.getDeliveryMethod(), actualResponse.getDeliveryMethod());
    }

    @Test
    void addOrder_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        OrderCreateRequest orderCreateRequest =  OrderCreateRequest.builder()
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY.name())
                .build();

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.addOrder(NON_EXISTING_USER_EMAIL, orderCreateRequest));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(orderMapper, never()).orderRequestToOrder(any(OrderCreateRequest.class), any(User.class));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderItemMapper, never()).cartItemToOrderItem(any(CartItem.class), any(Order.class), any(Product.class));
        verify(orderItemRepository, never()).saveAndFlush(any(OrderItem.class));
        verify(cartRepository, never()).deleteAll(any(Set.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void addOrder_shouldThrowDataNotFoundExceptionWhenUserCartIsEmpty() {

        OrderCreateRequest orderCreateRequest =  OrderCreateRequest.builder()
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY.name())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash("Hashed Password")
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .cart(Collections.emptySet())
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                orderService.addOrder(USER_EMAIL, orderCreateRequest));

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(orderMapper, never()).orderRequestToOrder(any(OrderCreateRequest.class), any(User.class));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderItemMapper, never()).cartItemToOrderItem(any(CartItem.class), any(Order.class), any(Product.class));
        verify(orderItemRepository, never()).saveAndFlush(any(OrderItem.class));
        verify(cartRepository, never()).deleteAll(any(Set.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("Cannot place order: user with email %s has an empty cart.", USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void updateOrder_shouldUpdateOrderSuccessfullyWhenOrderExistsAndCanBeUpdated() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("Updated Zip Code")
                .city("Updated City")
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CREATED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(orderUpdateRequest.getFirstName())
                .lastName(orderUpdateRequest.getLastName())
                .address(orderUpdateRequest.getAddress())
                .zipCode(orderUpdateRequest.getZipCode())
                .city(orderUpdateRequest.getCity())
                .phone(orderUpdateRequest.getPhone())
                .deliveryMethod(DeliveryMethod.valueOf(orderUpdateRequest.getDeliveryMethod()))
                .orderStatus(existingOrder.getOrderStatus())
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(updatedOrder.getOrderId())
                .firstName(updatedOrder.getFirstName())
                .lastName(updatedOrder.getLastName())
                .address(updatedOrder.getAddress())
                .zipCode(updatedOrder.getZipCode())
                .city(updatedOrder.getCity())
                .phone(updatedOrder.getPhone())
                .deliveryMethod(updatedOrder.getDeliveryMethod())
                .orderStatus(updatedOrder.getOrderStatus())
                .createdAt(updatedOrder.getCreatedAt())
                .updatedAt(updatedOrder.getUpdatedAt())
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);
        when(orderMapper.orderToResponse(updatedOrder)).thenReturn(orderResponse);

        OrderResponse actualResponse = orderService.updateOrder(USER_EMAIL, ORDER_ID.toString(), orderUpdateRequest);

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getOrderStatus(), capturedOrder.getOrderStatus());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(orderUpdateRequest.getFirstName(), capturedOrder.getFirstName());
        assertEquals(orderUpdateRequest.getLastName(), capturedOrder.getLastName());
        assertEquals(orderUpdateRequest.getPhone(), capturedOrder.getPhone());
        assertEquals(orderUpdateRequest.getDeliveryMethod(), capturedOrder.getDeliveryMethod().name());

        verify(orderMapper, times(1)).orderToResponse(updatedOrder);

        assertNotNull(actualResponse);
        assertEquals(orderResponse.getOrderId(), actualResponse.getOrderId());
        assertEquals(orderResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(orderResponse.getLastName(), actualResponse.getLastName());
        assertEquals(orderResponse.getDeliveryMethod(), actualResponse.getDeliveryMethod());
        assertEquals(orderResponse.getOrderStatus(), actualResponse.getOrderStatus());
        assertEquals(orderResponse.getCreatedAt(), actualResponse.getCreatedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingOrder.getUpdatedAt()));
    }

    @Test
    void updateOrder_shouldUpdateOnlyProvidedFieldsAndReturnUpdatedOrderResponse() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName(null)
                .lastName(null)
                .address(null)
                .zipCode(null)
                .city(null)
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CREATED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(orderUpdateRequest.getPhone())
                .deliveryMethod(DeliveryMethod.valueOf(orderUpdateRequest.getDeliveryMethod()))
                .orderStatus(existingOrder.getOrderStatus())
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(updatedOrder.getOrderId())
                .firstName(updatedOrder.getFirstName())
                .lastName(updatedOrder.getLastName())
                .address(updatedOrder.getAddress())
                .zipCode(updatedOrder.getZipCode())
                .city(updatedOrder.getCity())
                .phone(updatedOrder.getPhone())
                .deliveryMethod(updatedOrder.getDeliveryMethod())
                .orderStatus(updatedOrder.getOrderStatus())
                .createdAt(updatedOrder.getCreatedAt())
                .updatedAt(updatedOrder.getUpdatedAt())
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);
        when(orderMapper.orderToResponse(updatedOrder)).thenReturn(orderResponse);

        OrderResponse actualResponse = orderService.updateOrder(USER_EMAIL, ORDER_ID.toString(), orderUpdateRequest);

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getOrderStatus(), capturedOrder.getOrderStatus());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getFirstName(), capturedOrder.getFirstName());
        assertEquals(existingOrder.getLastName(), capturedOrder.getLastName());
        assertEquals(orderUpdateRequest.getPhone(), capturedOrder.getPhone());
        assertEquals(orderUpdateRequest.getDeliveryMethod(), capturedOrder.getDeliveryMethod().name());

        verify(orderMapper, times(1)).orderToResponse(updatedOrder);

        assertNotNull(actualResponse);
        assertEquals(orderResponse.getOrderId(), actualResponse.getOrderId());
        assertEquals(orderResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(orderResponse.getLastName(), actualResponse.getLastName());
        assertEquals(orderResponse.getDeliveryMethod(), actualResponse.getDeliveryMethod());
        assertEquals(orderResponse.getOrderStatus(), actualResponse.getOrderStatus());
        assertEquals(orderResponse.getCreatedAt(), actualResponse.getCreatedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingOrder.getUpdatedAt()));
    }

    @Test
    void updateOrder_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("Updated Zip Code")
                .city("Updated City")
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(USER_EMAIL, INVALID_ID, orderUpdateRequest));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));
    }

    @Test
    void updateUser_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("Updated Zip Code")
                .city("Updated City")
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderService.updateOrder(USER_EMAIL, NON_EXISTING_ORDER_ID.toString(), orderUpdateRequest));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderMapper, never()).orderToResponse(any(Order.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenOrderDoesNotBelongToUser() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("Updated Zip Code")
                .city("Updated City")
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email("anotherUser@example.com").build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () -> orderService.updateOrder(USER_EMAIL, ORDER_ID.toString(), orderUpdateRequest));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderMapper, never()).orderToResponse(existingOrder);

        assertEquals(String.format("Order with id: %s, does not belong to the user with email: %s.", ORDER_ID, USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenOrderCanNotBeUpdated() {

        OrderUpdateRequest orderUpdateRequest = OrderUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .address("Updated Address")
                .zipCode("Updated Zip Code")
                .city("Updated City")
                .phone("456")
                .deliveryMethod(CUSTOMER_PICKUP.name())
                .build();

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .address("Original Address")
                .zipCode("Original Zip Code")
                .city("Original City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(USER_EMAIL, ORDER_ID.toString(), orderUpdateRequest));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
        verify(orderMapper, never()).orderToResponse(existingOrder);

        assertEquals(String.format("Order with id: %s is already in status '%s' and can not be updated.", ORDER_ID, existingOrder.getOrderStatus().name()), thrownException.getMessage());
    }

    @Test
    void cancelOrder_shouldCancelOrderSuccessfullyWhenOrderExistsAndCanBeCanceled() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CREATED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(ORDER_STATUS_CANCELED)
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s was canceled.", ORDER_ID))
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);

        MessageResponse actualResponse = orderService.cancelOrder(USER_EMAIL, ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getDeliveryMethod(), capturedOrder.getDeliveryMethod());
        assertEquals(ORDER_STATUS_CANCELED, capturedOrder.getOrderStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void cancelOrder_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(USER_EMAIL, INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void cancelOrder_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderService.cancelOrder(USER_EMAIL, NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void cancelOrder_shouldThrowIllegalArgumentExceptionWhenOrderDoesNotBelongToUser() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email("anotherUser@example.com").build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(USER_EMAIL, ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));

        assertEquals(String.format("Order with id: %s, does not belong to the user with email: %s.", ORDER_ID, USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void cancelOrder_shouldThrowIllegalArgumentExceptionWhenOrderCanNotBeCanceled() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(USER_EMAIL, ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));

        assertEquals(String.format("Order with id: %s is already in status '%s' and can not be canceled.", ORDER_ID, existingOrder.getOrderStatus().name()), thrownException.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldChangeStatusFromCreatedToPaid() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CREATED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(ORDER_STATUS_PAID)
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s was updated from status '%s' to status '%s'.", ORDER_ID, ORDER_STATUS_CREATED, ORDER_STATUS_PAID))
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);

        MessageResponse actualResponse = orderService.toggleOrderStatus(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getDeliveryMethod(), capturedOrder.getDeliveryMethod());
        assertEquals(ORDER_STATUS_PAID, capturedOrder.getOrderStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldChangeStatusFromPaidToOnTheWay() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_PAID)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(ORDER_STATUS_ON_THE_WAY)
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s was updated from status '%s' to status '%s'.", ORDER_ID, ORDER_STATUS_PAID, ORDER_STATUS_ON_THE_WAY))
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);

        MessageResponse actualResponse = orderService.toggleOrderStatus(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getDeliveryMethod(), capturedOrder.getDeliveryMethod());
        assertEquals(ORDER_STATUS_ON_THE_WAY, capturedOrder.getOrderStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldChangeStatusFromOnTheWayToDelivered() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_ON_THE_WAY)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s was updated from status '%s' to status '%s'.", ORDER_ID, ORDER_STATUS_ON_THE_WAY, ORDER_STATUS_DELIVERED))
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);

        MessageResponse actualResponse = orderService.toggleOrderStatus(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getDeliveryMethod(), capturedOrder.getDeliveryMethod());
        assertEquals(ORDER_STATUS_DELIVERED, capturedOrder.getOrderStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldChangeStatusFromDeliveredToReturned() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_DELIVERED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        Order updatedOrder = Order.builder()
                .orderId(existingOrder.getOrderId())
                .firstName(existingOrder.getFirstName())
                .lastName(existingOrder.getLastName())
                .address(existingOrder.getAddress())
                .zipCode(existingOrder.getZipCode())
                .city(existingOrder.getCity())
                .phone(existingOrder.getPhone())
                .deliveryMethod(existingOrder.getDeliveryMethod())
                .orderStatus(ORDER_STATUS_RETURNED)
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingOrder.getUser())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Order with id: %s was updated from status '%s' to status '%s'.", ORDER_ID, ORDER_STATUS_DELIVERED, ORDER_STATUS_RETURNED))
                .build();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.saveAndFlush(existingOrder)).thenReturn(updatedOrder);

        MessageResponse actualResponse = orderService.toggleOrderStatus(ORDER_ID.toString());

        verify(orderRepository, times(1)).findById(ORDER_ID);

        verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder);
        assertEquals(existingOrder.getOrderId(), capturedOrder.getOrderId());
        assertEquals(existingOrder.getUser(), capturedOrder.getUser());
        assertEquals(existingOrder.getDeliveryMethod(), capturedOrder.getDeliveryMethod());
        assertEquals(ORDER_STATUS_RETURNED, capturedOrder.getOrderStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldThrowIllegalArgumentExceptionWhenOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> orderService.toggleOrderStatus(INVALID_ID));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void toggleOrderStatus_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderService.toggleOrderStatus(NON_EXISTING_ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void toggleOrderStatus_shouldThrowIllegalArgumentExceptionWhenOrderCanNotBeCanceled() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name")
                .lastName("Last Name")
                .address("Address")
                .zipCode("Zip Code")
                .city("City")
                .phone("123")
                .deliveryMethod(COURIER_DELIVERY)
                .orderStatus(ORDER_STATUS_CANCELED)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> orderService.toggleOrderStatus(ORDER_ID.toString()));

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderRepository, never()).saveAndFlush(any(Order.class));

        assertEquals(String.format("Order with id: %s is in final status %s and the status can not be changed.", ORDER_ID, existingOrder.getOrderStatus().name()), thrownException.getMessage());
    }
}