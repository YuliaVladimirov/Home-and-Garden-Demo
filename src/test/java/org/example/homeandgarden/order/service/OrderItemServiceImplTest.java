package org.example.homeandgarden.order.service;

import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.order.mapper.OrderItemMapper;
import org.example.homeandgarden.order.repository.OrderItemRepository;
import org.example.homeandgarden.order.repository.OrderRepository;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";
    private final String SORT_BY = "createdAt";

    private final UUID ORDER_ID = UUID.randomUUID();
    private final UUID NON_EXISTING_ORDER_ID = UUID.randomUUID();

    private final String INVALID_ID = "Invalid UUID";

    private final String USER_EMAIL = "user@example.com";

    private final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;

    private final Instant TIMESTAMP_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    @Test
    void getUserOrderItems_shouldReturnPagedOrderItemsWhenUserOrderExists() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        OrderItem orderItem1 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(3)
                .priceAtPurchase(BigDecimal.valueOf(40.00))
                .order(Order.builder().build())
                .product(product1)
                .build();


        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(20.00))
                .currentPrice(BigDecimal.valueOf(10.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(15.00))
                .order(Order.builder().build())
                .product(product2)
                .build();

        List<OrderItem> orderItems = List.of(orderItem1, orderItem2);
        Page<OrderItem> orderItemPage = new PageImpl<>(orderItems, pageRequest, orderItems.size());
        long expectedTotalPages = (long) Math.ceil((double) orderItems.size() / SIZE);


        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        OrderItemResponse orderItemResponse1 = OrderItemResponse.builder()
                .orderItemId(orderItem1.getOrderItemId())
                .quantity(orderItem1.getQuantity())
                .priceAtPurchase(orderItem1.getPriceAtPurchase())
                .product(productResponse1)
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(product2.getProductId())
                .productName(product2.getProductName())
                .listPrice(product2.getListPrice())
                .currentPrice(product2.getCurrentPrice())
                .productStatus(product2.getProductStatus())
                .addedAt(product2.getAddedAt())
                .updatedAt(product2.getUpdatedAt())
                .build();

        OrderItemResponse orderItemResponse2 = OrderItemResponse.builder()
                .orderItemId(orderItem2.getOrderItemId())
                .quantity(orderItem2.getQuantity())
                .priceAtPurchase(orderItem2.getPriceAtPurchase())
                .product(productResponse2)
                .build();

        when(orderRepository.existsByOrderId(ORDER_ID)).thenReturn(true);
        when(orderItemRepository.findByOrderOrderId(ORDER_ID, pageRequest)).thenReturn(orderItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(orderItemMapper.orderItemToResponse(orderItem1, productResponse1)).thenReturn(orderItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(orderItemMapper.orderItemToResponse(orderItem2, productResponse2)).thenReturn(orderItemResponse2);

        Page<OrderItemResponse> actualResponse = orderItemService.getUserOrderItems(ORDER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(orderRepository, times(1)).existsByOrderId(ORDER_ID);
        verify(orderItemRepository, times(1)).findByOrderOrderId(ORDER_ID, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(orderItemMapper, times(1)).orderItemToResponse(orderItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(orderItemMapper, times(1)).orderItemToResponse(orderItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(orderItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(orderItemResponse1.getOrderItemId(), actualResponse.getContent().getFirst().getOrderItemId());
        assertEquals(orderItemResponse2.getOrderItemId(), actualResponse.getContent().get(1).getOrderItemId());
    }

    @Test
    void getUserOrderItems_shouldThrowIllegalArgumentExceptionWhenUserOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                orderItemService.getUserOrderItems(INVALID_ID, SIZE, PAGE, ORDER, SORT_BY));

        verify(orderRepository, never()).existsByOrderId(any(UUID.class));
        verify(orderItemRepository, never()).findByOrderOrderId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(orderItemMapper, never()).orderItemToResponse(any(OrderItem.class), any(ProductResponse.class));
    }

    @Test
    void getUserOrderItems_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.existsByOrderId(NON_EXISTING_ORDER_ID)).thenReturn(false);

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderItemService.getUserOrderItems(NON_EXISTING_ORDER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY));

        verify(orderRepository, times(1)).existsByOrderId(NON_EXISTING_ORDER_ID);

        verify(orderItemRepository, never()).findByOrderOrderId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(orderItemMapper, never()).orderItemToResponse(any(OrderItem.class), any(ProductResponse.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

    @Test
    void getMyOrderItems_shouldReturnPagedOrderItemsWhenUserOrderExists() {

        Order existingOrder = Order.builder()
                .orderId(ORDER_ID)
                .firstName("First Name One")
                .lastName("Last Name One")
                .address("Address One")
                .zipCode("Zip Code One")
                .city("City One")
                .phone("123")
                .deliveryMethod(DeliveryMethod.COURIER_DELIVERY)
                .orderStatus(OrderStatus.PAID)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .build();

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        OrderItem orderItem1 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(3)
                .priceAtPurchase(BigDecimal.valueOf(40.00))
                .order(Order.builder().build())
                .product(product1)
                .build();

        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(20.00))
                .currentPrice(BigDecimal.valueOf(10.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(15.00))
                .order(Order.builder().build())
                .product(product2)
                .build();

        List<OrderItem> orderItems = List.of(orderItem1, orderItem2);
        Page<OrderItem> orderItemPage = new PageImpl<>(orderItems, pageRequest, orderItems.size());
        long expectedTotalPages = (long) Math.ceil((double) orderItems.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        OrderItemResponse orderItemResponse1 = OrderItemResponse.builder()
                .orderItemId(orderItem1.getOrderItemId())
                .quantity(orderItem1.getQuantity())
                .priceAtPurchase(orderItem1.getPriceAtPurchase())
                .product(productResponse1)
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(product2.getProductId())
                .productName(product2.getProductName())
                .listPrice(product2.getListPrice())
                .currentPrice(product2.getCurrentPrice())
                .productStatus(product2.getProductStatus())
                .addedAt(product2.getAddedAt())
                .updatedAt(product2.getUpdatedAt())
                .build();

        OrderItemResponse orderItemResponse2 = OrderItemResponse.builder()
                .orderItemId(orderItem2.getOrderItemId())
                .quantity(orderItem2.getQuantity())
                .priceAtPurchase(orderItem2.getPriceAtPurchase())
                .product(productResponse2)
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderItemRepository.findByOrderOrderId(ORDER_ID, pageRequest)).thenReturn(orderItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(orderItemMapper.orderItemToResponse(orderItem1, productResponse1)).thenReturn(orderItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(orderItemMapper.orderItemToResponse(orderItem2, productResponse2)).thenReturn(orderItemResponse2);

        Page<OrderItemResponse> actualResponse = orderItemService.getMyOrderItems(USER_EMAIL, ORDER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(orderRepository, times(1)).findById(ORDER_ID);
        verify(orderItemRepository, times(1)).findByOrderOrderId(ORDER_ID, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(orderItemMapper, times(1)).orderItemToResponse(orderItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(orderItemMapper, times(1)).orderItemToResponse(orderItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(orderItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(orderItemResponse1.getOrderItemId(), actualResponse.getContent().getFirst().getOrderItemId());
        assertEquals(orderItemResponse2.getOrderItemId(), actualResponse.getContent().get(1).getOrderItemId());
    }

    @Test
    void getMyOrderItems_shouldThrowIllegalArgumentExceptionWhenUserOrderIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                orderItemService.getMyOrderItems(USER_EMAIL, INVALID_ID, SIZE, PAGE, ORDER, SORT_BY));

        verify(orderRepository, never()).findById(any(UUID.class));
        verify(orderItemRepository, never()).findByOrderOrderId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(orderItemMapper, never()).orderItemToResponse(any(OrderItem.class), any(ProductResponse.class));
    }

    @Test
    void getMyOrderItems_shouldThrowDataNotFoundExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(NON_EXISTING_ORDER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> orderItemService.getMyOrderItems(USER_EMAIL, NON_EXISTING_ORDER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY));

        verify(orderRepository, times(1)).findById(NON_EXISTING_ORDER_ID);

        verify(orderItemRepository, never()).findByOrderOrderId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(orderItemMapper, never()).orderItemToResponse(any(OrderItem.class), any(ProductResponse.class));

        assertEquals(String.format("Order with id: %s, was not found.", NON_EXISTING_ORDER_ID), thrownException.getMessage());
    }

}