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
import org.springframework.data.web.PagedModel;

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

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";

    private final UUID PRODUCT_1_ID = UUID.randomUUID();
    private final UUID PRODUCT_2_ID = UUID.randomUUID();
    private final UUID WISH_LIST_ITEM_1_ID = UUID.randomUUID();
    private final UUID WISH_LIST_ITEM_2_ID = UUID.randomUUID();

    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_ID_STRING = USER_ID.toString();
    private final UUID NON_EXISTING_USER_ID = UUID.randomUUID();
    private final String NON_EXISTING_USER_ID_STRING = NON_EXISTING_USER_ID.toString();

    private final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private final String PASSWORD_HASH = "Hashed Password";

    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final String PRODUCT_ID_STRING = PRODUCT_ID.toString();
    private final UUID NON_EXISTING_PRODUCT_ID = UUID.randomUUID();
    private final String NON_EXISTING_PRODUCT_ID_STRING = NON_EXISTING_PRODUCT_ID.toString();
    private final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;
    private final ProductStatus PRODUCT_STATUS_OUT_OF_STOCK = ProductStatus.OUT_OF_STOCK;

    private final UUID WISH_LIST_ITEM_ID = UUID.randomUUID();
    private final String WISH_LIST_ITEM_ID_STRING = WISH_LIST_ITEM_ID.toString();
    private final UUID NON_EXISTING_WISH_LIST_ITEM_ID = UUID.randomUUID();
    private final String NON_EXISTING_WISH_LIST_ITEM_ID_STRING = NON_EXISTING_WISH_LIST_ITEM_ID.toString();

    private final String INVALID_ID = "Invalid UUID";

    private final Instant REGISTERED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant ADDED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant ADDED_AT_NOW = Instant.now();
    private final Instant UPDATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    @Test
    void getUserWishListItems_shouldReturnPagedWishListItemsWhenUserExists() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Product product1 = Product.builder()
                .productId(PRODUCT_1_ID)
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        WishListItem wishListItem1 = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_1_ID)
                .addedAt(ADDED_AT_PAST)
                .user(existingUser)
                .product(product1)
                .build();

        Product product2 = Product.builder()
                .productId(PRODUCT_2_ID)
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        WishListItem wishListItem2 = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_2_ID)
                .addedAt(ADDED_AT_PAST)
                .user(existingUser)
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

        PagedModel<WishListItemResponse> actualResponse = wishListService.getUserWishListItems(USER_ID_STRING, SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(wishListRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem1, productResponse1);
        verify(productMapper, times(1)).productToResponse(product2);
        verify(wishListMapper, times(1)).wishListItemToResponse(wishListItem2, productResponse2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(wishListItems.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

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

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.getUserWishListItems(NON_EXISTING_USER_ID_STRING, SIZE, PAGE, ORDER));

        verify(userRepository, times(1)).existsByUserId(NON_EXISTING_USER_ID);
        verify(wishListRepository, never()).findByUserUserId(any(UUID.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void getUserWishListItems_shouldReturnEmptyPagedModelWhenNoProductsMatchCriteria() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), "addedAt");

        Page<WishListItem> emptywishListItemPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(wishListRepository.findByUserUserId(USER_ID, pageRequest)).thenReturn(emptywishListItemPage);

        PagedModel<WishListItemResponse> actualResponse = wishListService.getUserWishListItems(USER_ID_STRING, SIZE, PAGE, ORDER);

        verify(userRepository, times(1)).existsByUserId(USER_ID);
        verify(wishListRepository, times(1)).findByUserUserId(USER_ID, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }


    @Test
    void addWishListItem_shouldAddWishListItemSuccessfully() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(USER_ID_STRING)
                .productId(PRODUCT_ID_STRING)
                .build();

        Product productToAdd = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product To Add")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
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
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Product existingProductInWishList = Product.builder()
                .productId(UUID.randomUUID()) // Different ID than PRODUCT_ID
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        WishListItem existingWishListItem = WishListItem.builder()
                .wishListItemId(UUID.randomUUID())
                .addedAt(ADDED_AT_PAST)
                .user(existingUser)
                .product(existingProductInWishList)
                .build();

        existingUser.setWishList(Set.of(existingWishListItem));

        WishListItem wishListItemToAdd = WishListItem.builder()
                .wishListItemId(null)
                .addedAt(null)
                .user(existingUser)
                .product(productToAdd)
                .build();

        WishListItem savedWishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(ADDED_AT_NOW)
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

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productToAdd));
        when(wishListMapper.requestToWishListItem(existingUser, productToAdd)).thenReturn(wishListItemToAdd);
        when(wishListRepository.saveAndFlush(wishListItemToAdd)).thenReturn(savedWishListItem);
        when(productMapper.productToResponse(productToAdd)).thenReturn(productResponse);
        when(wishListMapper.wishListItemToResponse(savedWishListItem, productResponse)).thenReturn(wishListItemResponse);

        WishListItemResponse actualResponse = wishListService.addWishListItem(wishListItemRequest);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, times(1)).requestToWishListItem(existingUser, productToAdd);

        verify(wishListRepository, times(1)).saveAndFlush(wishListItemCaptor.capture());
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
    void addWishListItem_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(INVALID_ID)
                .productId(PRODUCT_ID_STRING)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findById(any(UUID.class));
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));
    }

    @Test
    void addWishListItem_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(NON_EXISTING_USER_ID_STRING)
                .productId(PRODUCT_ID_STRING)
                .build();

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));


        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(USER_ID_STRING)
                .productId(INVALID_ID)
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
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, never()).findById(any(UUID.class));
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));
    }

    @Test
    void addWishListItem_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(USER_ID_STRING)
                .productId(NON_EXISTING_PRODUCT_ID_STRING)
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
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID_STRING), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowIllegalArgumentExceptionWhenProductIsNotAvailable() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(USER_ID_STRING)
                .productId(PRODUCT_ID_STRING)
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
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_OUT_OF_STOCK)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s has status '%s' and can not be added to the wish list.", PRODUCT_ID_STRING, PRODUCT_STATUS_OUT_OF_STOCK.name()), thrownException.getMessage());
    }

    @Test
    void addWishListItem_shouldThrowDataAlreadyExistsExceptionWhenProductIsAlreadyInWishList() {

        WishListItemRequest wishListItemRequest = WishListItemRequest.builder()
                .userId(USER_ID_STRING)
                .productId(PRODUCT_ID_STRING)
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
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        WishListItem wishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(ADDED_AT_PAST)
                .user(existingUser)
                .product(existingProduct)
                .build();

        Set<WishListItem> wishList = Set.of(wishListItem);
        existingUser.setWishList(wishList);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () -> wishListService.addWishListItem(wishListItemRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(wishListMapper, never()).requestToWishListItem(any(User.class), any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
        verify(wishListMapper, never()).wishListItemToResponse(any(WishListItem.class), any(ProductResponse.class));

        assertEquals(String.format("Product with id: %s is already in wish list.", PRODUCT_ID_STRING), thrownException.getMessage());
    }

    @Test
    void removeWishListItem_shouldRemoveWishListItemSuccessfully() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(REGISTERED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Product Name")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(ADDED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        WishListItem wishListItem = WishListItem.builder()
                .wishListItemId(WISH_LIST_ITEM_ID)
                .addedAt(ADDED_AT_PAST)
                .user(existingUser)
                .product(existingProduct)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", WISH_LIST_ITEM_ID_STRING))
                .build();

        when(wishListRepository.findById(WISH_LIST_ITEM_ID)).thenReturn(Optional.of(wishListItem));

        MessageResponse actualResponse = wishListService.removeWishListItem(WISH_LIST_ITEM_ID_STRING);

        verify(wishListRepository, times(1)).findById(WISH_LIST_ITEM_ID);
        verify(wishListRepository, times(1)).delete(wishListItem);

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void removeWishListItem_shouldThrowIllegalArgumentExceptionWhenWishListItemIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                wishListService.removeWishListItem(INVALID_ID));

        verify(wishListRepository, never()).findById(any(UUID.class));
        verify(wishListRepository, never()).delete(any(WishListItem.class));
    }

    @Test
    void removeWishListItem_shouldThrowDataNotFoundExceptionWhenWishListItemDoesNotExist() {

        when(wishListRepository.findById(NON_EXISTING_WISH_LIST_ITEM_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> wishListService.removeWishListItem(NON_EXISTING_WISH_LIST_ITEM_ID_STRING));

        verify(wishListRepository, times(1)).findById(NON_EXISTING_WISH_LIST_ITEM_ID);
        verify(wishListRepository, never()).deleteById(any(UUID.class));

        assertEquals(String.format("Wishlist item with id: %s, was not found.", NON_EXISTING_WISH_LIST_ITEM_ID_STRING), thrownException.getMessage());
    }
}