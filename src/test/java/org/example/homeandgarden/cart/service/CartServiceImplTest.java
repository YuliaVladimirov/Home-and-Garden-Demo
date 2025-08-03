package org.example.homeandgarden.cart.service;

import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.cart.mapper.CartMapper;
import org.example.homeandgarden.cart.repository.CartRepository;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.product.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";
    private final String SORT_BY = "addedAt";

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID NON_EXISTING_USER_ID = UUID.randomUUID();

    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID NON_EXISTING_PRODUCT_ID = UUID.randomUUID();

    private final UUID CART_ITEM_ID = UUID.randomUUID();
    private final UUID NON_EXISTING_CART_ITEM_ID = UUID.randomUUID();

    private final String INVALID_ID = "Invalid UUID";

    private final String USER_EMAIL = "user@example.com";
    private final String NON_EXISTING_USER_EMAIL = "nonExistingUser@example.com";

    private final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private final String PASSWORD_HASH = "Hashed Password";

    private final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;
    private final ProductStatus PRODUCT_STATUS_OUT_OF_STOCK = ProductStatus.OUT_OF_STOCK;

    private final Instant TIMESTAMP_NOW = Instant.now();
    private final Instant TIMESTAMP_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    @Test
    void getUserCartItems_shouldReturnPagedCartItemsWhenUserExists() {

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

        CartItem cartItem1 = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product1)
                .build();

        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem cartItem2 = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product2)
                .build();

        List<CartItem> cartItems = List.of(cartItem1, cartItem2);
        Page<CartItem> cartItemPage = new PageImpl<>(cartItems, pageRequest, cartItems.size());
        long expectedTotalPages = (long) Math.ceil((double) cartItems.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        CartItemResponse cartItemResponse1 = CartItemResponse.builder()
                .cartItemId(cartItem1.getCartItemId())
                .quantity(cartItem1.getQuantity())
                .addedAt(cartItem1.getAddedAt())
                .updatedAt(cartItem1.getUpdatedAt())
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

        CartItemResponse cartItemResponse2 = CartItemResponse.builder()
                .cartItemId(cartItem2.getCartItemId())
                .quantity(cartItem2.getQuantity())
                .addedAt(cartItem2.getAddedAt())
                .updatedAt(cartItem2.getUpdatedAt())
                .product(productResponse2)
                .build();

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(cartRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(cartItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(cartMapper.cartItemToResponse(cartItem1, productResponse1)).thenReturn(cartItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(cartMapper.cartItemToResponse(cartItem2, productResponse2)).thenReturn(cartItemResponse2);

        Page<CartItemResponse> actualResponse = cartService.getUserCartItems(USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(cartRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(cartMapper, times(1)).cartItemToResponse(cartItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(cartMapper, times(1)).cartItemToResponse(cartItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(cartItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(cartItemResponse1.getCartItemId(), actualResponse.getContent().getFirst().getCartItemId());
        assertEquals(cartItemResponse2.getCartItemId(), actualResponse.getContent().get(1).getCartItemId());
    }

    @Test
    void getUserCartItems_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                cartService.getUserCartItems(INVALID_ID, SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, never()).existsByUserId(any(UUID.class));
        verify(cartRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));
    }

    @Test
    void getUserCartItems_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.existsByUserId(NON_EXISTING_USER_ID)).thenReturn(false);

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.getUserCartItems(NON_EXISTING_USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, times(1)).existsByUserId(NON_EXISTING_USER_ID);
        verify(cartRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void getUserCartItems_shouldReturnEmptyPagedModelUserCartIsEmpty() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Page<CartItem> emptyCartItemPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(cartRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(emptyCartItemPage);

        Page<CartItemResponse> actualResponse = cartService.getUserCartItems(USER_ID.toString(), SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(cartRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

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
    void getMyCartItems_shouldReturnPagedCartItemsWhenUserExists() {

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

        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem cartItem1 = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product1)
                .build();

        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem cartItem2 = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(2)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product2)
                .build();

        List<CartItem> cartItems = List.of(cartItem1, cartItem2);
        Page<CartItem> cartItemPage = new PageImpl<>(cartItems, pageRequest, cartItems.size());
        long expectedTotalPages = (long) Math.ceil((double) cartItems.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        CartItemResponse cartItemResponse1 = CartItemResponse.builder()
                .cartItemId(cartItem1.getCartItemId())
                .quantity(cartItem1.getQuantity())
                .addedAt(cartItem1.getAddedAt())
                .updatedAt(cartItem1.getUpdatedAt())
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

        CartItemResponse cartItemResponse2 = CartItemResponse.builder()
                .cartItemId(cartItem2.getCartItemId())
                .quantity(cartItem2.getQuantity())
                .addedAt(cartItem2.getAddedAt())
                .updatedAt(cartItem2.getUpdatedAt())
                .product(productResponse2)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(cartRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(cartItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(cartMapper.cartItemToResponse(cartItem1, productResponse1)).thenReturn(cartItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(cartMapper.cartItemToResponse(cartItem2, productResponse2)).thenReturn(cartItemResponse2);

        Page<CartItemResponse> actualResponse = cartService.getMyCartItems(USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(cartRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(cartMapper, times(1)).cartItemToResponse(cartItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(cartMapper, times(1)).cartItemToResponse(cartItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(cartItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(cartItemResponse1.getCartItemId(), actualResponse.getContent().getFirst().getCartItemId());
        assertEquals(cartItemResponse2.getCartItemId(), actualResponse.getContent().get(1).getCartItemId());
    }

    @Test
    void getMyCartItems_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.getMyCartItems(NON_EXISTING_USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(cartRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void getMyCartItems_shouldReturnEmptyPagedModelUserCartIsEmpty() {

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

        Page<CartItem> emptyCartItemPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(cartRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(emptyCartItemPage);

        Page<CartItemResponse> actualResponse = cartService.getMyCartItems(USER_EMAIL, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(cartRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

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
    void addCartItem_shouldAddCartItemSuccessfully() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(USER_ID.toString())
                .productId(PRODUCT_ID.toString())
                .quantity(1)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
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
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .quantity(1)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(existingUser)
                .product(existingInCartProduct)
                .build();

        existingUser.setCart(Set.of(existingCartItem));

        Product productToAdd = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product To Add")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem cartItemToAdd = CartItem.builder()
                .cartItemId(null)
                .quantity(1)
                .addedAt(null)
                .updatedAt(null)
                .user(existingUser)
                .product(productToAdd)
                .build();

        CartItem addedCartItem = CartItem.builder()
                .cartItemId(CART_ITEM_ID)
                .quantity(1)
                .addedAt(TIMESTAMP_NOW)
                .updatedAt(TIMESTAMP_NOW)
                .user(existingUser)
                .product(productToAdd)
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(productToAdd.getProductId())
                .productName(productToAdd.getProductName())
                .listPrice(productToAdd.getListPrice())
                .currentPrice(productToAdd.getCurrentPrice())
                .productStatus(productToAdd.getProductStatus())
                .addedAt(productToAdd.getAddedAt())
                .updatedAt(productToAdd.getUpdatedAt())
                .build();

        CartItemResponse cartItemResponse = CartItemResponse.builder()
                .cartItemId(addedCartItem.getCartItemId())
                .quantity(addedCartItem.getQuantity())
                .addedAt(addedCartItem.getAddedAt())
                .updatedAt(addedCartItem.getUpdatedAt())
                .product(productResponse)
                .build();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productToAdd));
        when(cartMapper.requestToCartItem(cartItemCreateRequest, existingUser, productToAdd)).thenReturn(cartItemToAdd);
        when(cartRepository.saveAndFlush(cartItemToAdd)).thenReturn(addedCartItem);
        when(productMapper.productToResponse(productToAdd)).thenReturn(productResponse);
        when(cartMapper.cartItemToResponse(addedCartItem, productResponse)).thenReturn(cartItemResponse);

        CartItemResponse actualResponse = cartService.addCartItem(cartItemCreateRequest);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(cartMapper, times(1)).requestToCartItem(cartItemCreateRequest, existingUser, productToAdd);

        verify(cartRepository, times(1)).saveAndFlush(cartItemCaptor.capture());
        CartItem capturedCartItem = cartItemCaptor.getValue();
        assertNotNull(capturedCartItem);
        assertEquals(existingUser, capturedCartItem.getUser());
        assertEquals(productToAdd, capturedCartItem.getProduct());

        verify(productMapper, times(1)).productToResponse(productToAdd);
        verify(cartMapper, times(1)).cartItemToResponse(addedCartItem, productResponse);

        assertNotNull(actualResponse);
        assertEquals(cartItemResponse.getCartItemId(), actualResponse.getCartItemId());
        assertEquals(cartItemResponse.getAddedAt(), actualResponse.getAddedAt());
        assertEquals(cartItemResponse.getProduct(), actualResponse.getProduct());
    }

    @Test
    void addCartItem_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(INVALID_ID)
                .productId(PRODUCT_ID.toString())
                .quantity(1)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                cartService.addCartItem(cartItemCreateRequest));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findById(any(UUID.class));
        verify(cartMapper, never()).requestToCartItem(any(CartItemCreateRequest.class), any(User.class), any(Product.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));
    }

    @Test
    void addCartItem_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(NON_EXISTING_USER_ID.toString())
                .productId(PRODUCT_ID.toString())
                .quantity(1)
                .build();

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.addCartItem(cartItemCreateRequest));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(cartMapper, never()).requestToCartItem(any(CartItemCreateRequest.class), any(User.class), any(Product.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void addCartItem_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(USER_ID.toString())
                .productId(INVALID_ID)
                .quantity(1)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () ->
                cartService.addCartItem(cartItemCreateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(cartMapper, never()).requestToCartItem(any(CartItemCreateRequest.class), any(User.class), any(Product.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));
    }

    @Test
    void addCartItem_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(USER_ID.toString())
                .productId(NON_EXISTING_PRODUCT_ID.toString())
                .quantity(1)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.addCartItem(cartItemCreateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(cartMapper, never()).requestToCartItem(any(CartItemCreateRequest.class), any(User.class), any(Product.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void addCartItem_shouldThrowIllegalArgumentExceptionWhenProductIsNotAvailable() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(USER_ID.toString())
                .productId(PRODUCT_ID.toString())
                .quantity(1)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_OUT_OF_STOCK)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> cartService.addCartItem(cartItemCreateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(cartMapper, never()).requestToCartItem(any(CartItemCreateRequest.class), any(User.class), any(Product.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s has status '%s' and can not be added to the cart.", PRODUCT_ID, PRODUCT_STATUS_OUT_OF_STOCK.name()), thrownException.getMessage());
    }

    @Test
    void addCartItem_shouldIncreaseQuantityWhenProductIsAlreadyInWishList() {

        CartItemCreateRequest cartItemCreateRequest = CartItemCreateRequest.builder()
                .userId(USER_ID.toString())
                .productId(PRODUCT_ID.toString())
                .quantity(2)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product existingInCartProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .cartItemId(CART_ITEM_ID)
                .quantity(3)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(existingUser)
                .product(existingInCartProduct)
                .build();

        existingUser.setCart(Set.of(existingCartItem));

        CartItem addedCartItem = CartItem.builder()
                .cartItemId(CART_ITEM_ID)
                .quantity(5)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_NOW)
                .user(existingUser)
                .product(existingInCartProduct)
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(existingInCartProduct.getProductId())
                .productName(existingInCartProduct.getProductName())
                .listPrice(existingInCartProduct.getListPrice())
                .currentPrice(existingInCartProduct.getCurrentPrice())
                .productStatus(existingInCartProduct.getProductStatus())
                .addedAt(existingInCartProduct.getAddedAt())
                .updatedAt(existingInCartProduct.getUpdatedAt())
                .build();

        CartItemResponse cartItemResponse = CartItemResponse.builder()
                .cartItemId(addedCartItem.getCartItemId())
                .quantity(addedCartItem.getQuantity())
                .addedAt(addedCartItem.getAddedAt())
                .updatedAt(addedCartItem.getUpdatedAt())
                .product(productResponse)
                .build();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingInCartProduct));

        existingCartItem.setQuantity(5);

        when(cartRepository.saveAndFlush(existingCartItem)).thenReturn(addedCartItem);
        when(productMapper.productToResponse(existingInCartProduct)).thenReturn(productResponse);
        when(cartMapper.cartItemToResponse(addedCartItem, productResponse)).thenReturn(cartItemResponse);

        CartItemResponse actualResponse = cartService.addCartItem(cartItemCreateRequest);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);

        verify(cartRepository, times(1)).saveAndFlush(cartItemCaptor.capture());
        CartItem capturedCartItem = cartItemCaptor.getValue();
        assertNotNull(capturedCartItem);
        assertEquals(existingUser, capturedCartItem.getUser());
        assertEquals(existingInCartProduct, capturedCartItem.getProduct());

        verify(productMapper, times(1)).productToResponse(existingInCartProduct);
        verify(cartMapper, times(1)).cartItemToResponse(addedCartItem, productResponse);

        assertNotNull(actualResponse);
        assertEquals(cartItemResponse.getCartItemId(), actualResponse.getCartItemId());
        assertEquals(cartItemResponse.getQuantity(), actualResponse.getQuantity());
        assertEquals(cartItemResponse.getProduct(), actualResponse.getProduct());
        assertEquals(cartItemResponse.getAddedAt(), actualResponse.getAddedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingCartItem.getUpdatedAt()));
    }

    @Test
    void updateCartItem_shouldUpdateCartItemSuccessfullyWhenCartItemExists() {

        CartItemUpdateRequest cartItemUpdateRequest = CartItemUpdateRequest.builder()
                .quantity(3)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .cartItemId(CART_ITEM_ID)
                .quantity(1)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(existingProduct)
                .build();

        CartItem updatedCartItem = CartItem.builder()
                .cartItemId(existingCartItem.getCartItemId())
                .quantity(cartItemUpdateRequest.getQuantity())
                .addedAt(existingCartItem.getAddedAt())
                .updatedAt(TIMESTAMP_NOW)
                .user(existingCartItem.getUser())
                .product(existingCartItem.getProduct())
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(existingProduct.getProductId())
                .productName(existingProduct.getProductName())
                .listPrice(existingProduct.getListPrice())
                .currentPrice(existingProduct.getCurrentPrice())
                .productStatus(existingProduct.getProductStatus())
                .addedAt(existingProduct.getAddedAt())
                .updatedAt(existingProduct.getUpdatedAt())
                .build();

        CartItemResponse cartItemResponse = CartItemResponse.builder()
                .cartItemId(updatedCartItem.getCartItemId())
                .quantity(updatedCartItem.getQuantity())
                .addedAt(updatedCartItem.getAddedAt())
                .updatedAt(updatedCartItem.getUpdatedAt())
                .product(productResponse)
                .build();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        when(cartRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(existingCartItem));
        existingCartItem.setQuantity(cartItemUpdateRequest.getQuantity());
        when(cartRepository.saveAndFlush(existingCartItem)).thenReturn(updatedCartItem);
        when(cartMapper.cartItemToResponse(updatedCartItem, productResponse)).thenReturn(cartItemResponse);
        when(productMapper.productToResponse(existingProduct)).thenReturn(productResponse);

        CartItemResponse actualResponse = cartService.updateCartItem(CART_ITEM_ID.toString(), cartItemUpdateRequest);

        verify(cartRepository, times(1)).findById(CART_ITEM_ID);

        verify(cartRepository, times(1)).saveAndFlush(cartItemCaptor.capture());
        CartItem capturedCartItem = cartItemCaptor.getValue();
        assertNotNull(capturedCartItem);
        assertEquals(cartItemUpdateRequest.getQuantity(), capturedCartItem.getQuantity());
        assertEquals(existingCartItem.getUser(), capturedCartItem.getUser());
        assertEquals(existingCartItem.getProduct(), capturedCartItem.getProduct());

        verify(productMapper, times(1)).productToResponse(existingProduct);
        verify(cartMapper, times(1)).cartItemToResponse(updatedCartItem, productResponse);

        assertNotNull(actualResponse);
        assertEquals(cartItemResponse.getCartItemId(), actualResponse.getCartItemId());
        assertEquals(cartItemResponse.getQuantity(), actualResponse.getQuantity());
        assertEquals(cartItemResponse.getProduct(), actualResponse.getProduct());
        assertEquals(cartItemResponse.getAddedAt(), actualResponse.getAddedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingCartItem.getUpdatedAt()));
    }

    @Test
    void updateCartItem_shouldThrowIllegalArgumentExceptionWhenCartItemIdIsInvalidUuidString() {

        CartItemUpdateRequest cartItemUpdateRequest = CartItemUpdateRequest.builder()
                .quantity(3)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                cartService.updateCartItem(INVALID_ID, cartItemUpdateRequest));

        verify(cartRepository, never()).findById(any(UUID.class));
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));
    }

    @Test
    void updateCartItem_shouldThrowDataNotFoundExceptionWhenCartItemDoesNotExist() {

        CartItemUpdateRequest cartItemUpdateRequest = CartItemUpdateRequest.builder()
                .quantity(3)
                .build();

        when(cartRepository.findById(NON_EXISTING_CART_ITEM_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.updateCartItem(NON_EXISTING_CART_ITEM_ID.toString(), cartItemUpdateRequest));

        verify(cartRepository, times(1)).findById(NON_EXISTING_CART_ITEM_ID);
        verify(cartRepository, never()).saveAndFlush(any(CartItem.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(cartMapper, never()).cartItemToResponse(any(CartItem.class), any(ProductResponse.class));

        assertEquals(String.format("Cart item with id: %s, was not found.", NON_EXISTING_CART_ITEM_ID), thrownException.getMessage());
    }


    @Test
    void removeCarItem_shouldRemoveCartItemSuccessfully() {

        CartItem existingCartItem = CartItem.builder()
                .cartItemId(CART_ITEM_ID)
                .quantity(1)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(Product.builder().build())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Cart item with id: %s, has been removed from cart.", CART_ITEM_ID))
                .build();

        when(cartRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(existingCartItem));

        MessageResponse actualResponse = cartService.removeCarItem(CART_ITEM_ID.toString());

        verify(cartRepository, times(1)).findById(CART_ITEM_ID);
        verify(cartRepository, times(1)).delete(existingCartItem);

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void removeCarItem_shouldThrowIllegalArgumentExceptionWhenCartItemIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                cartService.removeCarItem(INVALID_ID));

        verify(cartRepository, never()).findById(CART_ITEM_ID);
        verify(cartRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void removeCarItem_shouldThrowDataNotFoundExceptionWhenCartItemDoesNotExist() {

        when(cartRepository.findById(NON_EXISTING_CART_ITEM_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> cartService.removeCarItem(NON_EXISTING_CART_ITEM_ID.toString()));

        verify(cartRepository, times(1)).findById(NON_EXISTING_CART_ITEM_ID);
        verify(cartRepository, never()).delete(any(CartItem.class));

        assertEquals(String.format("Cart item with id: %s, was not found.", NON_EXISTING_CART_ITEM_ID), thrownException.getMessage());
    }
}