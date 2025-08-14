package org.example.homeandgarden.product.service;

import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.category.repository.CategoryRepository;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.product.dto.*;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.ProductProjection;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.product.repository.ProductRepository;
import org.example.homeandgarden.shared.MessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 5;
    private static final String ORDER = "ASC";
    private static final String SORT_BY = "productName";

    private static final UUID CATEGORY_ID = UUID.fromString("d167268d-305b-426e-9f6f-998da4c2ff76");
    private static final UUID NON_EXISTING_CATEGORY_ID = UUID.fromString("a0f3bd4e-2c9c-4c23-a9b7-e4b2d99d36e5");

    private static final UUID PRODUCT_1_ID = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
    private static final UUID PRODUCT_2_ID = UUID.fromString("e9b1d3b0-146a-4be0-a1e2-2b9e18b1a8cf");

    private static final UUID PRODUCT_ID = UUID.fromString("aebdd1a2-1bc2-4cc9-8496-674e4c7ee5f2");
    private static final UUID NON_EXISTING_PRODUCT_ID = UUID.fromString("de305d54-75b4-431b-adb2-eb6b9e546014");

    private static final String INVALID_ID = "Invalid UUID";

    private static final CategoryStatus CATEGORY_STATUS_ACTIVE = CategoryStatus.ACTIVE;
    private static final CategoryStatus CATEGORY_STATUS_INACTIVE = CategoryStatus.INACTIVE;

    private static final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;
    private static final ProductStatus PRODUCT_STATUS_OUT_OF_STOCK = ProductStatus.OUT_OF_STOCK;

    private static final String INVALID_STATUS = "Invalid Status";

    private final Instant TIMESTAMP_NOW = Instant.now();
    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(10.0);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(50.0);

    @Test
    void getCategoryProducts_shouldReturnPagedProductsWhenCategoryExistsAndProductsMatchCriteria() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product product1 = Product.builder()
                .productId(PRODUCT_1_ID)
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(existingCategory)
                .build();

        Product product2 = Product.builder()
                .productId(PRODUCT_2_ID)
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(existingCategory)
                .build();

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
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

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, MIN_PRICE, MAX_PRICE, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        Page<ProductResponse> actualResponse = productService.getCategoryProducts(CATEGORY_ID.toString(), MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, MIN_PRICE, MAX_PRICE, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(products.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(2, actualResponse.getContent().size());
        assertEquals(productResponse1.getProductId(), actualResponse.getContent().getFirst().getProductId());
        assertEquals(productResponse1.getProductName(), actualResponse.getContent().getFirst().getProductName());
        assertEquals(productResponse1.getProductStatus(), actualResponse.getContent().getFirst().getProductStatus());
        assertEquals(productResponse2.getProductId(), actualResponse.getContent().get(1).getProductId());
        assertEquals(productResponse2.getProductName(), actualResponse.getContent().get(1).getProductName());
        assertEquals(productResponse2.getProductStatus(), actualResponse.getContent().get(1).getProductStatus());
    }

    @Test
    void getCategoryProducts_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                productService.getCategoryProducts(INVALID_ID, MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getCategoryProducts_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.getCategoryProducts(NON_EXISTING_CATEGORY_ID.toString(), MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldThrowIllegalArgumentExceptionWhenCategoryIsDisabled() {

        Category disabledCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Disabled Category")
                .categoryStatus(CATEGORY_STATUS_INACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(disabledCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.getCategoryProducts(CATEGORY_ID.toString(), MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, is disabled.", CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldReturnEmptyPagedModelWhenNoProductsMatchCriteria() {

        BigDecimal minPrice = BigDecimal.valueOf(200.00); // Price range that won't match
        BigDecimal maxPrice = BigDecimal.valueOf(300.00);

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);
        Page<Product> emptyProductPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, minPrice, maxPrice, pageRequest)).thenReturn(emptyProductPage);

        Page<ProductResponse> actualResponse = productService.getCategoryProducts(CATEGORY_ID.toString(), minPrice, maxPrice, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, minPrice, maxPrice, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));

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
    void getProductsByStatus_shouldRetrieveAndMapProductsByCertainStatusWithPagination() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Product product1 = Product.builder()
                .productId(PRODUCT_1_ID)
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        Product product2 = Product.builder()
                .productId(PRODUCT_2_ID)
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
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

        when(productRepository.findAllByProductStatus(PRODUCT_STATUS_AVAILABLE, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        Page<ProductResponse> actualResponse = productService.getProductsByStatus(PRODUCT_STATUS_AVAILABLE.name(), SIZE, PAGE, ORDER, SORT_BY);

        verify(productRepository, times(1)).findAllByProductStatus(PRODUCT_STATUS_AVAILABLE, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(products.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(products.size(), actualResponse.getContent().size());
        assertEquals(productResponse1.getProductId(), actualResponse.getContent().getFirst().getProductId());
        assertEquals(productResponse1.getProductName(), actualResponse.getContent().getFirst().getProductName());
        assertEquals(productResponse1.getProductStatus(), actualResponse.getContent().getFirst().getProductStatus());
        assertEquals(productResponse2.getProductId(), actualResponse.getContent().get(1).getProductId());
        assertEquals(productResponse2.getProductName(), actualResponse.getContent().get(1).getProductName());
        assertEquals(productResponse2.getProductStatus(), actualResponse.getContent().get(1).getProductStatus());
    }

    @Test
    void getProductsByStatus_shouldRetrieveAndMapAllProductsWithPaginationIfNoStatusIsProvided() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Product product1 = Product.builder()
                .productId(PRODUCT_1_ID)
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        Product product2 = Product.builder()
                .productId(PRODUCT_2_ID)
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_OUT_OF_STOCK)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(product1.getProductId())
                .productName(product1.getProductName())
                .listPrice(product1.getListPrice())
                .currentPrice(product1.getCurrentPrice())
                .productStatus(product1.getProductStatus())
                .addedAt(product1.getAddedAt())
                .updatedAt(product1.getUpdatedAt())
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

        when(productRepository.findAll(pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        Page<ProductResponse> actualResponse = productService.getProductsByStatus(null, SIZE, PAGE, ORDER, SORT_BY);

        verify(productRepository, times(1)).findAll(pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(products.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(products.size(), actualResponse.getContent().size());
        assertEquals(productResponse1.getProductId(), actualResponse.getContent().getFirst().getProductId());
        assertEquals(productResponse1.getProductName(), actualResponse.getContent().getFirst().getProductName());
        assertEquals(productResponse1.getProductStatus(), actualResponse.getContent().getFirst().getProductStatus());
        assertEquals(productResponse2.getProductId(), actualResponse.getContent().get(1).getProductId());
        assertEquals(productResponse2.getProductName(), actualResponse.getContent().get(1).getProductName());
        assertEquals(productResponse2.getProductStatus(), actualResponse.getContent().get(1).getProductStatus());
    }

    @Test
    void getProductsByStatus_shouldThrowIllegalArgumentExceptionWhenProductStatusIsInvalidString() {

        assertThrows(IllegalArgumentException.class, () ->
                productService.getProductsByStatus(INVALID_STATUS, SIZE, PAGE, ORDER, SORT_BY));

        verify(productRepository, never()).findAllByProductStatus(any(ProductStatus.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForPaidStatusWithPagination() {

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.PAID, OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED);

        ProductProjection productProjection1 = ProductProjection.builder()
                .productId(PRODUCT_1_ID)
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(56L)
                .build();

        ProductProjection productProjection2 = ProductProjection.builder()
                .productId(PRODUCT_2_ID)
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(39L)
                .build();

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);

        ProductProjectionResponse productProjectionResponse1 = ProductProjectionResponse.builder()
                .productId(productProjection1.getProductId())
                .productName(productProjection1.getProductName())
                .listPrice(productProjection1.getListPrice())
                .currentPrice(productProjection1.getCurrentPrice())
                .productStatus(productProjection1.getProductStatus())
                .addedAt(productProjection1.getAddedAt())
                .updatedAt(productProjection1.getUpdatedAt())
                .totalAmount(productProjection1.getTotalAmount())
                .build();

        ProductProjectionResponse productProjectionResponse2 = ProductProjectionResponse.builder()
                .productId(productProjection2.getProductId())
                .productName(productProjection2.getProductName())
                .listPrice(productProjection2.getListPrice())
                .currentPrice(productProjection2.getCurrentPrice())
                .productStatus(productProjection2.getProductStatus())
                .addedAt(productProjection2.getAddedAt())
                .updatedAt(productProjection2.getUpdatedAt())
                .totalAmount(productProjection2.getTotalAmount())
                .build();

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        Page<ProductProjectionResponse> actualResponse = productService.getTopProducts("PAID", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForCanceledStatusWithPagination() {

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.CANCELED, OrderStatus.RETURNED);

        ProductProjection productProjection1 = ProductProjection.builder()
                .productId(PRODUCT_1_ID)
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(56L)
                .build();

        ProductProjection productProjection2 = ProductProjection.builder()
                .productId(PRODUCT_2_ID)
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(39L)
                .build();

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);

        ProductProjectionResponse productProjectionResponse1 = ProductProjectionResponse.builder()
                .productId(productProjection1.getProductId())
                .productName(productProjection1.getProductName())
                .listPrice(productProjection1.getListPrice())
                .currentPrice(productProjection1.getCurrentPrice())
                .productStatus(productProjection1.getProductStatus())
                .addedAt(productProjection1.getAddedAt())
                .updatedAt(productProjection1.getUpdatedAt())
                .totalAmount(productProjection1.getTotalAmount())
                .build();

        ProductProjectionResponse productProjectionResponse2 = ProductProjectionResponse.builder()
                .productId(productProjection2.getProductId())
                .productName(productProjection2.getProductName())
                .listPrice(productProjection2.getListPrice())
                .currentPrice(productProjection2.getCurrentPrice())
                .productStatus(productProjection2.getProductStatus())
                .addedAt(productProjection2.getAddedAt())
                .updatedAt(productProjection2.getUpdatedAt())
                .totalAmount(productProjection2.getTotalAmount())
                .build();

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        Page<ProductProjectionResponse> actualResponse = productService.getTopProducts("CANCELED", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getTopProducts_shouldReturnEmptyPagedModelIfNoTopProductsFound() {

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.CANCELED, OrderStatus.RETURNED);
        Page<ProductProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(productRepository.findTopProducts(expectedStatuses, pageRequest)).thenReturn(emptyPage);

        Page<ProductProjectionResponse> actualResponse = productService.getTopProducts("CANCELED", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(expectedStatuses, pageRequest);
        verify(productMapper, never()).productProjectionToResponse(any(ProductProjection.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertTrue(actualResponse.getContent().isEmpty());
    }

    @Test
    void getPendingProducts_shouldRetrieveAndMapAllPendingProductsWithPaginationIfProductsExistForGivenStatusAndDays() {

        OrderStatus orderStatus = OrderStatus.PAID;
        String orderStatusString = orderStatus.name();
        Integer days = 7;

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);

        ProductProjection productProjection1 = ProductProjection.builder()
                .productId(PRODUCT_1_ID)
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(56L)
                .build();

        ProductProjection productProjection2 = ProductProjection.builder()
                .productId(PRODUCT_2_ID)
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .totalAmount(39L)
                .build();

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);

        ProductProjectionResponse productProjectionResponse1 = ProductProjectionResponse.builder()
                .productId(productProjection1.getProductId())
                .productName(productProjection1.getProductName())
                .listPrice(productProjection1.getListPrice())
                .currentPrice(productProjection1.getCurrentPrice())
                .productStatus(productProjection1.getProductStatus())
                .addedAt(productProjection1.getAddedAt())
                .updatedAt(productProjection1.getUpdatedAt())
                .totalAmount(productProjection1.getTotalAmount())
                .build();

        ProductProjectionResponse productProjectionResponse2 = ProductProjectionResponse.builder()
                .productId(productProjection2.getProductId())
                .productName(productProjection2.getProductName())
                .listPrice(productProjection2.getListPrice())
                .currentPrice(productProjection2.getCurrentPrice())
                .productStatus(productProjection2.getProductStatus())
                .addedAt(productProjection2.getAddedAt())
                .updatedAt(productProjection2.getUpdatedAt())
                .totalAmount(productProjection2.getTotalAmount())
                .build();

        when(productRepository.findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest))).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        Page<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatusString, days, SIZE, PAGE);

        verify(productRepository, times(1)).findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long) SIZE, actualResponse.getSize());
        assertEquals((long) PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getPendingProducts_shouldReturnEmptyPagedModelWhenNoPendingProductsMatch() {

        OrderStatus orderStatus = OrderStatus.PAID;
        String orderStatusString = orderStatus.name();
        Integer days = 7;

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        Page<ProductProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(productRepository.findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest))).thenReturn(emptyPage);

        Page<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatusString, days, SIZE, PAGE);

        verify(productRepository, times(1)).findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest));
        verify(productMapper, never()).productProjectionToResponse(any(ProductProjection.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertTrue(actualResponse.getContent().isEmpty());
    }

    @Test
    void getPendingProducts_shouldThrowIllegalArgumentExceptionWhenOrderStatusStringIsInvalid() {

        String invalidOrderStatus = "INVALID_STATUS";
        Integer days = 5;

        assertThrows(IllegalArgumentException.class, () -> productService.getPendingProducts(invalidOrderStatus, days, SIZE, PAGE));

        verify(productRepository, never()).findPendingProducts(any(OrderStatus.class), any(Instant.class), any(PageRequest.class));
        verify(productMapper, never()).productProjectionToResponse(any(ProductProjection.class));
    }

    @Test
    void getProfitByPeriod_shouldReturnProfitForDayPeriod() {

        String timeUnit = "DAY";
        Integer timePeriod = 5;
        BigDecimal expectedProfit = BigDecimal.valueOf(123.45);

        when(productRepository.findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class))).thenReturn(expectedProfit);

        ProductProfitResponse actualResponse = productService.getProfitByPeriod(timeUnit, timePeriod);

        verify(productRepository, times(1)).findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class));

        assertNotNull(actualResponse);
        assertEquals(timeUnit.toUpperCase(), actualResponse.getTimeUnit());
        assertEquals(timePeriod, actualResponse.getTimePeriod());
        assertEquals(expectedProfit, actualResponse.getProfit());
    }

    @Test
    void getProfitByPeriod_shouldReturnProfitForWeekPeriod() {

        String timeUnit = "WEEK";
        Integer timePeriod = 2;
        BigDecimal expectedProfit = BigDecimal.valueOf(500.00);

        when(productRepository.findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class))).thenReturn(expectedProfit);

        ProductProfitResponse actualResponse = productService.getProfitByPeriod(timeUnit, timePeriod);

        verify(productRepository, times(1)).findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class));

        assertNotNull(actualResponse);
        assertEquals(timeUnit.toUpperCase(), actualResponse.getTimeUnit());
        assertEquals(timePeriod, actualResponse.getTimePeriod());
        assertEquals(expectedProfit, actualResponse.getProfit());
    }

    @Test
    void getProfitByPeriod_shouldReturnProfitForMonthPeriod() {

        String timeUnit = "MONTH";
        Integer timePeriod = 3;
        BigDecimal expectedProfit = BigDecimal.valueOf(1500.75);

        when(productRepository.findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class))).thenReturn(expectedProfit);

        ProductProfitResponse actualResponse = productService.getProfitByPeriod(timeUnit, timePeriod);

        verify(productRepository, times(1)).findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class));

        assertNotNull(actualResponse);
        assertEquals(timeUnit.toUpperCase(), actualResponse.getTimeUnit());
        assertEquals(timePeriod, actualResponse.getTimePeriod());
        assertEquals(expectedProfit, actualResponse.getProfit());
    }

    @Test
    void getProfitByPeriod_shouldReturnProfitForYearPeriod() {

        String timeUnit = "YEAR";
        Integer timePeriod = 1;
        BigDecimal expectedProfit = BigDecimal.valueOf(10000.00);

        when(productRepository.findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class))).thenReturn(expectedProfit);

        ProductProfitResponse actualResponse = productService.getProfitByPeriod(timeUnit, timePeriod);

        verify(productRepository, times(1)).findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class));

        assertNotNull(actualResponse);
        assertEquals(timeUnit.toUpperCase(), actualResponse.getTimeUnit());
        assertEquals(timePeriod, actualResponse.getTimePeriod());
        assertEquals(expectedProfit, actualResponse.getProfit());
    }

    @Test
    void getProfitByPeriod_shouldReturnZeroProfitWhenRepositoryReturnsZero() {

        String timeUnit = "DAY";
        Integer timePeriod = 1;
        BigDecimal expectedProfit = BigDecimal.ZERO;

        when(productRepository.findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class))).thenReturn(expectedProfit);

        ProductProfitResponse actualResponse = productService.getProfitByPeriod(timeUnit, timePeriod);

        verify(productRepository, times(1)).findProfitByPeriod(eq(OrderStatus.DELIVERED), any(Instant.class));

        assertNotNull(actualResponse);
        assertEquals(timeUnit.toUpperCase(), actualResponse.getTimeUnit());
        assertEquals(timePeriod, actualResponse.getTimePeriod());
        assertEquals(expectedProfit, actualResponse.getProfit());
    }

    @Test
    void getProfitByPeriod_shouldThrowIllegalArgumentExceptionForInvalidTimeUnit() {

        String invalidTimeUnit = "QUARTER";
        Integer timePeriod = 1;

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.getProfitByPeriod(invalidTimeUnit, timePeriod));

        verify(productRepository, never()).findProfitByPeriod(any(OrderStatus.class), any(Instant.class));

        assertEquals(String.format("Unexpected value: %s", invalidTimeUnit), thrownException.getMessage());
    }

    @Test
    void getProductById_shouldReturnProductResponseWhenProductExists() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Existing Product")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
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

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productMapper.productToResponse(existingProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.getProductById(PRODUCT_ID.toString());

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productMapper, times(1)).productToResponse(existingProduct);

        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
    }

    @Test
    void getProductById_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.getProductById(NON_EXISTING_PRODUCT_ID.toString()));

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void getProductById_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(INVALID_ID));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldAddProductSuccessfully() {

        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
                .categoryId(CATEGORY_ID.toString())
                .productName("New Product")
                .listPrice(BigDecimal.valueOf(10.00))
                .build();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Product productToSave = Product.builder()
                .productId(null)
                .productName(productCreateRequest.getProductName())
                .listPrice(productCreateRequest.getListPrice())
                .currentPrice(productCreateRequest.getListPrice())
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(null)
                .updatedAt(null)
                .build();

        Product addedProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName(productToSave.getProductName())
                .listPrice(productToSave.getListPrice())
                .currentPrice(productToSave.getCurrentPrice())
                .productStatus(productToSave.getProductStatus())
                .addedAt(TIMESTAMP_NOW)
                .updatedAt(TIMESTAMP_NOW)
                .category(productToSave.getCategory())
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(addedProduct.getProductId())
                .productName(addedProduct.getProductName())
                .listPrice(addedProduct.getListPrice())
                .currentPrice(addedProduct.getCurrentPrice())
                .productStatus(addedProduct.getProductStatus())
                .addedAt(addedProduct.getAddedAt())
                .updatedAt(addedProduct.getUpdatedAt())
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(productMapper.requestToProduct(productCreateRequest, existingCategory)).thenReturn(productToSave);
        when(productRepository.save(productToSave)).thenReturn(addedProduct);
        when(productMapper.productToResponse(addedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.addProduct(productCreateRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productMapper, times(1)).requestToProduct(productCreateRequest, existingCategory);

        verify(productRepository, times(1)).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertNotNull(capturedProduct);
        assertEquals(productCreateRequest.getProductName(), capturedProduct.getProductName());
        assertEquals(productCreateRequest.getListPrice(), capturedProduct.getListPrice());
        assertEquals(PRODUCT_STATUS_AVAILABLE, capturedProduct.getProductStatus());

        verify(productMapper, times(1)).productToResponse(addedProduct);

        assertNotNull(actualResponse);
        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
        assertEquals(productResponse.getAddedAt(), actualResponse.getAddedAt());
        assertEquals(productResponse.getUpdatedAt(), actualResponse.getUpdatedAt());
    }

    @Test
    void addProduct_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
                .categoryId(INVALID_ID)
                .productName("New Product")
                .listPrice(BigDecimal.valueOf(10.00))
                .build();

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
                .categoryId(NON_EXISTING_CATEGORY_ID.toString())
                .productName("New Product")
                .listPrice(BigDecimal.valueOf(10.00))
                .build();

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void addProduct_shouldThrowIllegalArgumentExceptionWhenCategoryStatusIsInactive() {

        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
                .categoryId(CATEGORY_ID.toString())
                .productName("New Product")
                .listPrice(BigDecimal.valueOf(10.00))
                .build();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_INACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void updateProduct_shouldUpdateProductSuccessfullyWhenProductExistsAndIsNotSoldOut() {

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Name")
                .listPrice(BigDecimal.valueOf(15.00))
                .currentPrice(BigDecimal.valueOf(10.00))
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        Product updatedProduct = Product.builder()
                .productId(existingProduct.getProductId())
                .productName(updateRequest.getProductName())
                .listPrice(updateRequest.getListPrice())
                .currentPrice(updateRequest.getCurrentPrice())
                .productStatus(existingProduct.getProductStatus())
                .addedAt(existingProduct.getAddedAt())
                .updatedAt(TIMESTAMP_NOW)
                .category(existingProduct.getCategory())
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(updatedProduct.getProductId())
                .productName(updatedProduct.getProductName())
                .listPrice(updatedProduct.getListPrice())
                .currentPrice(updatedProduct.getCurrentPrice())
                .productStatus(updatedProduct.getProductStatus())
                .addedAt(updatedProduct.getAddedAt())
                .updatedAt(updatedProduct.getUpdatedAt())
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(PRODUCT_ID.toString(), updateRequest);

        verify(productRepository, times(1)).findById(PRODUCT_ID);

        verify(productRepository, times(1)).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertNotNull(capturedProduct);
        assertEquals(existingProduct.getProductId(), capturedProduct.getProductId());
        assertEquals(updateRequest.getProductName(), capturedProduct.getProductName());
        assertEquals(updateRequest.getListPrice(), capturedProduct.getListPrice());
        assertEquals(updateRequest.getCurrentPrice(), capturedProduct.getCurrentPrice());
        assertEquals(existingProduct.getProductStatus(), capturedProduct.getProductStatus());

        verify(productMapper, times(1)).productToResponse(updatedProduct);

        assertNotNull(actualResponse);
        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getListPrice(), actualResponse.getListPrice());
        assertEquals(productResponse.getCurrentPrice(), actualResponse.getCurrentPrice());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
        assertEquals(productResponse.getAddedAt(), actualResponse.getAddedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingProduct.getUpdatedAt()));
    }

    @Test
    void updateProduct_shouldUpdateOnlyProvidedFieldsAndReturnUpdatedProductResponse() {

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Name")
                .listPrice(null)
                .currentPrice(null)
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        Product updatedProduct = Product.builder()
                .productId(existingProduct.getProductId())
                .productName(updateRequest.getProductName())
                .listPrice(existingProduct.getListPrice())
                .currentPrice(existingProduct.getCurrentPrice())
                .productStatus(existingProduct.getProductStatus())
                .addedAt(existingProduct.getAddedAt())
                .updatedAt(TIMESTAMP_NOW)
                .category(existingProduct.getCategory())
                .build();

        ProductResponse productResponse = ProductResponse.builder()
                .productId(updatedProduct.getProductId())
                .productName(updatedProduct.getProductName())
                .listPrice(updatedProduct.getListPrice())
                .currentPrice(updatedProduct.getCurrentPrice())
                .productStatus(updatedProduct.getProductStatus())
                .addedAt(updatedProduct.getAddedAt())
                .updatedAt(updatedProduct.getUpdatedAt())
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(PRODUCT_ID.toString(), updateRequest);

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, times(1)).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertEquals(existingProduct.getProductId(), capturedProduct.getProductId());
        assertEquals(updateRequest.getProductName(), capturedProduct.getProductName());
        assertEquals(existingProduct.getListPrice(), capturedProduct.getListPrice());
        assertEquals(existingProduct.getCurrentPrice(), capturedProduct.getCurrentPrice());
        assertEquals(existingProduct.getProductStatus(), capturedProduct.getProductStatus());

        verify(productMapper, times(1)).productToResponse(updatedProduct);

        assertNotNull(actualResponse);
        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getListPrice(), actualResponse.getListPrice());
        assertEquals(productResponse.getCurrentPrice(), actualResponse.getCurrentPrice());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
        assertEquals(productResponse.getAddedAt(), actualResponse.getAddedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingProduct.getUpdatedAt()));
    }

    @Test
    void updateProduct_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Name")
                .listPrice(BigDecimal.valueOf(15.00))
                .currentPrice(BigDecimal.valueOf(15.00))
                .build();

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.updateProduct(NON_EXISTING_PRODUCT_ID.toString(), updateRequest));

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIsSoldOut() {

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Name")
                .listPrice(BigDecimal.valueOf(15.00))
                .currentPrice(BigDecimal.valueOf(15.00))
                .build();

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(ProductStatus.SOLD_OUT)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(PRODUCT_ID.toString(), updateRequest));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, is sold out and can not be updated.", PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Name")
                .listPrice(BigDecimal.valueOf(15.00))
                .currentPrice(BigDecimal.valueOf(15.00))
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                productService.updateProduct(INVALID_ID, updateRequest));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void setProductStatus_shouldSetProductStatusSuccessfullyWhenProductExistsAndStatusIsDifferent() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        Product updatedProduct = Product.builder()
                .productId(existingProduct.getProductId())
                .productName(existingProduct.getProductName())
                .listPrice(existingProduct.getListPrice())
                .currentPrice(existingProduct.getCurrentPrice())
                .productStatus(PRODUCT_STATUS_OUT_OF_STOCK)
                .addedAt(existingProduct.getAddedAt())
                .updatedAt(TIMESTAMP_NOW)
                .category(existingProduct.getCategory())
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for the product with id: %s.", PRODUCT_STATUS_OUT_OF_STOCK.name(), PRODUCT_ID))
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(updatedProduct);

        MessageResponse actualResponse = productService.setProductStatus(PRODUCT_ID.toString(), PRODUCT_STATUS_OUT_OF_STOCK.name());

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, times(1)).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();

        assertEquals(existingProduct.getProductId(), capturedProduct.getProductId());
        assertEquals(existingProduct.getProductName(), capturedProduct.getProductName());
        assertEquals(PRODUCT_STATUS_OUT_OF_STOCK, capturedProduct.getProductStatus());
        assertTrue(updatedProduct.getUpdatedAt().isAfter(existingProduct.getUpdatedAt()));

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(INVALID_ID, PRODUCT_STATUS_OUT_OF_STOCK.name()));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.setProductStatus(NON_EXISTING_PRODUCT_ID.toString(), PRODUCT_STATUS_OUT_OF_STOCK.name()));

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productRepository, never()).save(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID), thrownException.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductStatusIsInvalid() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(PRODUCT_STATUS_AVAILABLE)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(PRODUCT_ID.toString(), INVALID_STATUS));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductAlreadyHasTargetStatus() {

        Product existingProduct = Product.builder()
                .productId(PRODUCT_ID)
                .productName("Original Name")
                .listPrice(BigDecimal.valueOf(25.00))
                .currentPrice(BigDecimal.valueOf(25.00))
                .productStatus(PRODUCT_STATUS_OUT_OF_STOCK)
                .addedAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .category(Category.builder().build())
                .build();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(PRODUCT_ID.toString(), PRODUCT_STATUS_OUT_OF_STOCK.name()));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).save(any(Product.class));

        assertEquals(String.format("Product with id: %s, already has status '%s'.", PRODUCT_ID, PRODUCT_STATUS_OUT_OF_STOCK.name()), thrownException.getMessage());
    }
}