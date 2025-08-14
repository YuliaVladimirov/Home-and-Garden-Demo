package org.example.homeandgarden.wishlist.service;

import org.example.homeandgarden.exception.DataAlreadyExistsException;
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
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.example.homeandgarden.wishlist.mapper.WishListMapper;
import org.example.homeandgarden.wishlist.repository.WishListRepository;
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
class WishListServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishListRepository wishListRepository;

    @Mock
    private WishListMapper wishListMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private WishListServiceImpl wishListService;

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 5;
    private static final String ORDER = "ASC";

    private static final UUID USER_ID = UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76");
    private static final UUID NON_EXISTING_USER_ID = UUID.fromString("28123116-33f4-4b89-8519-b22ac2350834");

    private static final UUID PRODUCT_ID = UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76");
    private static final UUID NON_EXISTING_PRODUCT_ID = UUID.fromString("a0f3bd4e-2c9c-4c23-a9b7-e4b2d99d36e5");

    private static final UUID WISH_LIST_ITEM_ID = UUID.fromString("f8b4b6f0-1a2e-4d30-b5a7-9c4e7a1e8c3d");
    private static final UUID NON_EXISTING_WISH_LIST_ITEM_ID = UUID.fromString("3a6e2d4b-5f14-46a0-90e3-8212f6bd93b9");

    private static final String INVALID_ID = "Invalid UUID";

    private static final String USER_EMAIL = "user@example.com";
    private static final String NON_EXISTING_USER_EMAIL = "nonExistingUser@example.com";

    private static final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private static final String PASSWORD_HASH = "Hashed Password";

    private static final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;
    private static final ProductStatus PRODUCT_STATUS_OUT_OF_STOCK = ProductStatus.OUT_OF_STOCK;

    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

    @Test
    void getUserWishListItems_shouldReturnPagedWishListItemsWhenUserExists() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem wishListItem1 = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(TIMESTAMP_PAST)
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

        WishListItem wishListItem2 = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product2)
                .build();

        List<WishListItem> wishListItems = List.of(wishListItem1, wishListItem2);
        Page<WishListItem> wishListItemPage = new PageImpl<>(wishListItems, pageRequest, wishListItems.size());
        long expectedTotalPages = (long) Math.ceil((double) wishListItems.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        WishListItemResponse wishListItemResponse1 = WishListItemResponse.builder()
                .wishListItemId(wishListItem1.getWishListItemId())
                .addedAt(wishListItem1.getAddedAt())
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

        WishListItemResponse wishListItemResponse2 = WishListItemResponse.builder()
                .wishListItemId(wishListItem2.getWishListItemId())
                .addedAt(wishListItem2.getAddedAt())
                .product(productResponse2)
                .build();

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(wishListRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(wishListItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(wishListMapper.wishListItemToResponse(wishListItem1, productResponse1)).thenReturn(wishListItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(wishListMapper.wishListItemToResponse(wishListItem2, productResponse2)).thenReturn(wishListItemResponse2);

        Page<WishListItemResponse> actualResponse = wishListService.getUserWishListItems(USER_ID.toString(), SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(wishListRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(wishListItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(wishListItemResponse1.getWishListItemId(), actualResponse.getContent().getFirst().getWishListItemId());
        assertEquals(wishListItemResponse2.getWishListItemId(), actualResponse.getContent().get(1).getWishListItemId());
    }

    @Test
    void getUserWishListItems_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.getUserWishListItems(INVALID_ID, SIZE, PAGE, ORDER));

        verify(userRepository, never()).existsByUserId(any(UUID.class));
        verify(wishListRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));
    }

    @Test
    void getUserWishListItems_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.existsByUserId(NON_EXISTING_USER_ID)).thenReturn(false);

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.getUserWishListItems(NON_EXISTING_USER_ID.toString(), SIZE, PAGE, ORDER));

        verify(userRepository, times(1)).existsByUserId(NON_EXISTING_USER_ID);
        verify(wishListRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void getUserWishListItems_shouldReturnEmptyPagedModelUserWishListIsEmpty() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        Page<WishListItem> emptyWishListItemPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(wishListRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(emptyWishListItemPage);

        Page<WishListItemResponse> actualResponse = wishListService.getUserWishListItems(USER_ID.toString(), SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(wishListRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

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
    void getMyWishListItems_shouldReturnPagedWishListItemsWhenUserExists() {

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

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem wishListItem1 = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(TIMESTAMP_PAST)
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

        WishListItem wishListItem2 = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(TIMESTAMP_PAST)
                .user(User.builder().build())
                .product(product2)
                .build();

        List<WishListItem> wishListItems = List.of(wishListItem1, wishListItem2);
        Page<WishListItem> wishListItemPage = new PageImpl<>(wishListItems, pageRequest, wishListItems.size());
        long expectedTotalPages = (long) Math.ceil((double) wishListItems.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
                .build();

        WishListItemResponse wishListItemResponse1 = WishListItemResponse.builder()
                .wishListItemId(wishListItem1.getWishListItemId())
                .addedAt(wishListItem1.getAddedAt())
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

        WishListItemResponse wishListItemResponse2 = WishListItemResponse.builder()
                .wishListItemId(wishListItem2.getWishListItemId())
                .addedAt(wishListItem2.getAddedAt())
                .product(productResponse2)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(wishListRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(wishListItemPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(wishListMapper.wishListItemToResponse(wishListItem1, productResponse1)).thenReturn(wishListItemResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);
        when(wishListMapper.wishListItemToResponse(wishListItem2, productResponse2)).thenReturn(wishListItemResponse2);

        Page<WishListItemResponse> actualResponse = wishListService.getMyWishListItems(USER_EMAIL, SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(wishListRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(wishListItems.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(wishListItemResponse1.getWishListItemId(), actualResponse.getContent().getFirst().getWishListItemId());
        assertEquals(wishListItemResponse2.getWishListItemId(), actualResponse.getContent().get(1).getWishListItemId());
    }

    @Test
    void getMyWishListItems_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.getMyWishListItems(NON_EXISTING_USER_EMAIL, SIZE, PAGE, ORDER));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(wishListRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void getMyWishListItems_shouldReturnEmptyPagedModelUserWishListIsEmpty() {

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

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        Page<WishListItem> emptyWishListItemPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(wishListRepository.findByUserUserId(existingUser.getUserId(), pageRequest)).thenReturn(emptyWishListItemPage);

        Page<WishListItemResponse> actualResponse = wishListService.getMyWishListItems(USER_EMAIL, SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(wishListRepository, times(1)).findByUserUserId(existingUser.getUserId(), pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

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
    void addWishListItem_shouldAddWishListItemSuccessfully() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(PRODUCT_ID.toString())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product existingInWishListProduct = Product.builder()
                .productId(UUID.randomUUID())
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem existingWishListItem = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(TIMESTAMP_PAST)
                .user(existingUser)
                .product(existingInWishListProduct)
                .build();

        existingUser.setWishList(Set.of(existingWishListItem));

        Product productToAdd = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product To Add")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem wishListItemToAdd = WishListItem.builder()
                .wishListItemId(null)
                .addedAt(null)
                .user(existingUser)
                .product(productToAdd)
                .build();

        WishListItem savedWishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(Instant.now())
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

        WishListItemResponse wishListItemResponse = WishListItemResponse.builder()
                .wishListItemId(savedWishListItem.getWishListItemId())
                .addedAt(savedWishListItem.getAddedAt())
                .product(productResponse)
                .build();

        ArgumentCaptor<WishListItem> wishListItemCaptor = ArgumentCaptor.forClass(WishListItem.class);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productToAdd));
        when(wishListMapper.requestToWishListItem(existingUser, productToAdd)).thenReturn(wishListItemToAdd);
        when(wishListRepository.save(wishListItemToAdd)).thenReturn(savedWishListItem);
        when(productMapper.productToResponse(productToAdd)).thenReturn(productResponse);
        when(wishListMapper.wishListItemToResponse(savedWishListItem, productResponse)).thenReturn(wishListItemResponse);

        WishListItemResponse actualResponse = wishListService.addWishListItem(USER_EMAIL, wishListItemRequest);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, times(1)).requestToWishListItem(existingUser, productToAdd);

        verify(wishListRepository, times(1)).save(wishListItemCaptor.capture());
        WishListItem capturedWishListItem = wishListItemCaptor.getValue();
        assertNotNull(capturedWishListItem);
        assertEquals(existingUser, capturedWishListItem.getUser());
        assertEquals(productToAdd, capturedWishListItem.getProduct());

        verify(productMapper, times(1)).productToResponse(productToAdd);
        verify(wishListMapper, times(1)).wishListItemToResponse(savedWishListItem, productResponse);

        assertNotNull(actualResponse);
        assertEquals(wishListItemResponse.getWishListItemId(), actualResponse.getWishListItemId());
        assertEquals(wishListItemResponse.getAddedAt(), actualResponse.getAddedAt());
        assertEquals(wishListItemResponse.getProduct(), actualResponse.getProduct());
    }

    @Test
    void addWishListItem_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(PRODUCT_ID.toString())
                .build();

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.addWishListItem(NON_EXISTING_USER_EMAIL, wishListItemRequest));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(INVALID_ID)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.addWishListItem(USER_EMAIL, wishListItemRequest));

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));
    }

    @Test
    void addWishListItem_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(NON_EXISTING_PRODUCT_ID.toString())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.addWishListItem(USER_EMAIL, wishListItemRequest));

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowIllegalArgumentExceptionWhenProductIsNotAvailable() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(PRODUCT_ID.toString())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> wishListService.addWishListItem(USER_EMAIL, wishListItemRequest));

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s has status '%s' and can not be added to the wish list.", PRODUCT_ID, PRODUCT_STATUS_OUT_OF_STOCK.name()), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowDataAlreadyExistsExceptionWhenProductIsAlreadyInWishList() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .productId(PRODUCT_ID.toString())
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product existingInWishListProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem existingWishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(TIMESTAMP_PAST)
                .user(existingUser)
                .product(existingInWishListProduct)
                .build();

        existingUser.setWishList(Set.of(existingWishListItem));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingInWishListProduct));

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () -> wishListService.addWishListItem(USER_EMAIL, wishListItemRequest));

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s is already in wish list.", PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void removeWishListItem_shouldRemoveWishListItemSuccessfully() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem existingWishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(TIMESTAMP_PAST)
                .user(User.builder().email(USER_EMAIL).build())
                .product(existingProduct)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", WISH_LIST_ITEM_ID))
                .build();

        when(wishListRepository.findById(WISH_LIST_ITEM_ID)).thenReturn(Optional.of(existingWishListItem));

        MessageResponse actualResponse = wishListService.removeWishListItem(USER_EMAIL, WISH_LIST_ITEM_ID.toString());

        verify(wishListRepository, times(1)).findById(WISH_LIST_ITEM_ID);
        verify(wishListRepository, times(1)).delete(existingWishListItem);

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void removeWishListItem_shouldThrowIllegalArgumentExceptionWhenWishListItemIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.removeWishListItem(USER_EMAIL, INVALID_ID));

        verify(wishListRepository, never()).findById(any(UUID.class));
        verify(wishListRepository, never()).delete(any(WishListItem.class));
    }

    @Test
    void removeWishListItem_shouldThrowDataNotFoundExceptionWhenWishListItemDoesNotExist() {

        when(wishListRepository.findById(NON_EXISTING_WISH_LIST_ITEM_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.removeWishListItem(USER_EMAIL, NON_EXISTING_WISH_LIST_ITEM_ID.toString()));

        verify(wishListRepository, times(1)).findById(NON_EXISTING_WISH_LIST_ITEM_ID);
        verify(wishListRepository, never()).deleteById(any(UUID.class));

        assertEquals(String.format("Wishlist item with id: %s, was not found.", NON_EXISTING_WISH_LIST_ITEM_ID), thrownException.getMessage());
    }

    @Test
    void removeWishListItem_shouldThrowAccessDeniedExceptionWhenWishListItemDoesNotBelongToUser() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        WishListItem existingWishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(TIMESTAMP_PAST)
                .user(User.builder().email("anotherUser@example.com").build())
                .product(existingProduct)
                .build();

        when(wishListRepository.findById(WISH_LIST_ITEM_ID)).thenReturn(Optional.of(existingWishListItem));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () -> wishListService.removeWishListItem(USER_EMAIL, WISH_LIST_ITEM_ID.toString()));

        verify(wishListRepository, times(1)).findById(WISH_LIST_ITEM_ID);
        verify(wishListRepository, never()).delete(any(WishListItem.class));

        assertEquals(String.format("Wishlist item with id: %s, does not belong to the wishlist of the user with email: %s.", WISH_LIST_ITEM_ID, USER_EMAIL), thrownException.getMessage());
    }
}