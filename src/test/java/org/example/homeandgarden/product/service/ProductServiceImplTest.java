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
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";
    private final String SORT_BY = "productName";

    private final BigDecimal MIN_PRICE = BigDecimal.valueOf(10.0);
    private final BigDecimal MAX_PRICE = BigDecimal.valueOf(50.0);

    private final UUID PRODUCT_1_ID = UUID.randomUUID();
    private final UUID PRODUCT_2_ID = UUID.randomUUID();

    private final UUID CATEGORY_ID = UUID.randomUUID();
    private final String CATEGORY_ID_STRING = CATEGORY_ID.toString();
    private final UUID NON_EXISTING_CATEGORY_ID = UUID.randomUUID();
    private final String NON_EXISTING_CATEGORY_ID_STRING = NON_EXISTING_CATEGORY_ID.toString();
    private final String INVALID_CATEGORY_ID = "Invalid UUID";

    private final CategoryStatus CATEGORY_STATUS_ACTIVE = CategoryStatus.ACTIVE;
    private final CategoryStatus CATEGORY_STATUS_INACTIVE = CategoryStatus.INACTIVE;

    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final String PRODUCT_ID_STRING = PRODUCT_ID.toString();
    private final UUID NON_EXISTING_PRODUCT_ID = UUID.randomUUID();
    private final String NON_EXISTING_PRODUCT_ID_STRING = NON_EXISTING_PRODUCT_ID.toString();
    private final String INVALID_PRODUCT_ID = "Invalid UUID";

    private final ProductStatus PRODUCT_STATUS_AVAILABLE = ProductStatus.AVAILABLE;
    private final String PRODUCT_STATUS_AVAILABLE_STRING = PRODUCT_STATUS_AVAILABLE.name();
    private final ProductStatus PRODUCT_STATUS_OUT_OF_STOCK = ProductStatus.OUT_OF_STOCK;
    private final String PRODUCT_STATUS_OUT_OF_STOCK_STRING = PRODUCT_STATUS_OUT_OF_STOCK.name();
    private final String INVALID_PRODUCT_STATUS = "Invalid Status";

    private final Instant ADDED_AT_NOW = Instant.now();
    private final Instant UPDATED_AT_NOW = Instant.now();
    private final Instant CREATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant ADDED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant UPDATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    private ProductCreateRequest createProductCreateRequest(String id, String productName, BigDecimal listPrice) {
        return ProductCreateRequest.builder()
                .categoryId(id)
                .productName(productName)
                .listPrice(listPrice)
                .build();
    }

    private ProductUpdateRequest createProductUpdateRequest(String productName, BigDecimal listPrice, BigDecimal currentPrice) {
        return ProductUpdateRequest.builder()
                .productName(productName)
                .listPrice(listPrice)
                .currentPrice(currentPrice)
                .build();
    }

    private Category createCategory(UUID id, String categoryName, CategoryStatus categoryStatus, Instant createdAt, Instant updatedAt) {
        return Category.builder()
                .categoryId(id)
                .categoryName(categoryName)
                .categoryStatus(categoryStatus)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private Product createProduct(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt) {
        return Product.builder()
                .productId(id)
                .productName(productName)
                .listPrice(listPrice)
                .currentPrice(currentPrice)
                .productStatus(productStatus)
                .addedAt(addedAt)
                .updatedAt(updatedAt)
                .build();
    }

    private ProductResponse createProductResponse(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt) {
        return ProductResponse.builder()
                .productId(id)
                .productName(productName)
                .listPrice(listPrice)
                .currentPrice(currentPrice)
                .productStatus(productStatus)
                .addedAt(addedAt)
                .updatedAt(updatedAt)
                .build();
    }

    private ProductProjection createProductProjection(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt, Long totalAmount) {
        return ProductProjection.builder()
                .productId(id)
                .productName(productName)
                .listPrice(listPrice)
                .currentPrice(currentPrice)
                .productStatus(productStatus)
                .addedAt(addedAt)
                .updatedAt(updatedAt)
                .totalAmount(totalAmount)
                .build();
    }

    private ProductProjectionResponse createProductProjectionResponse(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt, Long totalAmount) {
        return ProductProjectionResponse.builder()
                .productId(id)
                .productName(productName)
                .listPrice(listPrice)
                .currentPrice(currentPrice)
                .productStatus(productStatus)
                .addedAt(addedAt)
                .updatedAt(updatedAt)
                .totalAmount(totalAmount)
                .build();
    }


    @Test
    void getCategoryProducts_shouldReturnPagedProductsWhenCategoryExistsAndProductsMatchCriteria() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category category = createCategory(CATEGORY_ID, "Existing Category", CATEGORY_STATUS_ACTIVE, CREATED_AT_PAST, UPDATED_AT_PAST);

        Product product1 = createProduct(PRODUCT_1_ID, "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);
        Product product2 = createProduct(PRODUCT_2_ID, "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, MIN_PRICE, MAX_PRICE, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getCategoryProducts(CATEGORY_ID_STRING, MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, MIN_PRICE, MAX_PRICE, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

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
                productService.getCategoryProducts(INVALID_CATEGORY_ID, MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getCategoryProducts_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.getCategoryProducts(NON_EXISTING_CATEGORY_ID_STRING, MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID_STRING), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldThrowIllegalArgumentExceptionWhenCategoryIsDisabled() {

        Category disabledCategory = createCategory(CATEGORY_ID, "Disabled Category", CATEGORY_STATUS_INACTIVE, CREATED_AT_PAST, UPDATED_AT_PAST);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(disabledCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.getCategoryProducts(CATEGORY_ID_STRING, MIN_PRICE, MAX_PRICE, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, is disabled.", CATEGORY_ID_STRING), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldReturnEmptyPagedModelWhenNoProductsMatchCriteria() {

        BigDecimal minPrice = BigDecimal.valueOf(200.00); // Price range that won't match
        BigDecimal maxPrice = BigDecimal.valueOf(300.00);

        Category category = createCategory(CATEGORY_ID, "Existing Category", CATEGORY_STATUS_ACTIVE, CREATED_AT_PAST, UPDATED_AT_PAST);

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);
        Page<Product> emptyProductPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, minPrice, maxPrice, pageRequest)).thenReturn(emptyProductPage);

        PagedModel<ProductResponse> actualResponse = productService.getCategoryProducts(CATEGORY_ID_STRING, minPrice, maxPrice, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                CATEGORY_ID, PRODUCT_STATUS_AVAILABLE, minPrice, maxPrice, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));

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
    void getProductsByStatus_shouldRetrieveAndMapProductsByCertainStatusWithPagination() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Product product1 = createProduct(PRODUCT_1_ID, "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);
        Product product2 = createProduct(PRODUCT_2_ID, "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(productRepository.findAllByProductStatus(PRODUCT_STATUS_AVAILABLE, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getProductsByStatus(PRODUCT_STATUS_AVAILABLE_STRING, SIZE, PAGE, ORDER, SORT_BY);

        verify(productRepository, times(1)).findAllByProductStatus(PRODUCT_STATUS_AVAILABLE, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

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

        Product product1 = createProduct(UUID.randomUUID(), "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);
        Product product2 = createProduct(UUID.randomUUID(), "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / SIZE);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(productRepository.findAll(pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getProductsByStatus(null, SIZE, PAGE, ORDER, SORT_BY);

        verify(productRepository, times(1)).findAll(pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

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
                productService.getProductsByStatus(INVALID_PRODUCT_STATUS, SIZE, PAGE, ORDER, SORT_BY));

        verify(productRepository, never()).findAllByProductStatus(any(ProductStatus.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForPaidStatusWithPagination() {

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.PAID, OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);

        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts("PAID", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForCanceledStatusWithPagination() {

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.CANCELED, OrderStatus.RETURNED);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);


        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts("CANCELED", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

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

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts("CANCELED", SIZE, PAGE);

        verify(productRepository, times(1)).findTopProducts(expectedStatuses, pageRequest);
        verify(productMapper, never()).productProjectionToResponse(any(ProductProjection.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertTrue(actualResponse.getContent().isEmpty());
    }

    @Test
    void getPendingProducts_shouldRetrieveAndMapAllPendingProductsWithPaginationIfProductsExistForGivenStatusAndDays() {

        OrderStatus orderStatus = OrderStatus.PAID;
        String orderStatusString = orderStatus.name();
        Integer days = 7;

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST, 39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / SIZE);

        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest))).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatusString, days, SIZE, PAGE);

        verify(productRepository, times(1)).findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long) SIZE, actualResponse.getMetadata().size());
        assertEquals((long) PAGE, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getPendingProducts_shouldReturnEmptyPagedModelIfNoPendingProductsMatch() {

        OrderStatus orderStatus = OrderStatus.PAID;
        String orderStatusString = orderStatus.name();
        Integer days = 7;

        PageRequest pageRequest = PageRequest.of(PAGE, SIZE);
        Page<ProductProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(productRepository.findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest))).thenReturn(emptyPage);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatusString, days, SIZE, PAGE);

        verify(productRepository, times(1)).findPendingProducts(eq(orderStatus), any(Instant.class), eq(pageRequest));
        verify(productMapper, never()).productProjectionToResponse(any(ProductProjection.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
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

        Product product = createProduct(PRODUCT_ID, "Existing Product", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        ProductResponse productResponse = createProductResponse(product.getProductId(), product.getProductName(), product.getListPrice(), product.getCurrentPrice(), product.getProductStatus(), product.getAddedAt(), product.getUpdatedAt());

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productMapper.productToResponse(product)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.getProductById(PRODUCT_ID_STRING);

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productMapper, times(1)).productToResponse(product);

        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
    }

    @Test
    void getProductById_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.getProductById(NON_EXISTING_PRODUCT_ID_STRING));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID_STRING), thrownException.getMessage());

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getProductById_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(INVALID_PRODUCT_ID));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldAddProductSuccessfully() {

        ProductCreateRequest productCreateRequest = createProductCreateRequest(CATEGORY_ID_STRING, "New Product", BigDecimal.valueOf(10.00));

        Category category = createCategory(CATEGORY_ID, "Existing Category", CATEGORY_STATUS_ACTIVE, CREATED_AT_PAST, UPDATED_AT_PAST);

        Product productToSave = createProduct(null, productCreateRequest.getProductName(), productCreateRequest.getListPrice(), productCreateRequest.getListPrice(), PRODUCT_STATUS_AVAILABLE, null, null);

        Product savedProduct = createProduct(PRODUCT_ID, productToSave.getProductName(), productToSave.getListPrice(), productToSave.getCurrentPrice(), productToSave.getProductStatus(), ADDED_AT_NOW, UPDATED_AT_NOW);

        ProductResponse productResponse = createProductResponse(savedProduct.getProductId(), savedProduct.getProductName(), savedProduct.getListPrice(), savedProduct.getCurrentPrice(), savedProduct.getProductStatus(), savedProduct.getAddedAt(), savedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productMapper.requestToProduct(productCreateRequest, category)).thenReturn(productToSave);
        when(productRepository.saveAndFlush(productToSave)).thenReturn(savedProduct);
        when(productMapper.productToResponse(savedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.addProduct(productCreateRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productMapper, times(1)).requestToProduct(productCreateRequest, category);

        verify(productRepository, times(1)).saveAndFlush(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertNotNull(capturedProduct);
        assertEquals(productCreateRequest.getProductName(), capturedProduct.getProductName());
        assertEquals(productCreateRequest.getListPrice(), capturedProduct.getListPrice());
        assertEquals(PRODUCT_STATUS_AVAILABLE, capturedProduct.getProductStatus());

        verify(productMapper, times(1)).productToResponse(savedProduct);

        assertNotNull(actualResponse);
        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
        assertEquals(productResponse.getAddedAt(), actualResponse.getAddedAt());
        assertEquals(productResponse.getUpdatedAt(), actualResponse.getUpdatedAt());
    }

    @Test
    void addProduct_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        ProductCreateRequest productCreateRequest = createProductCreateRequest(INVALID_CATEGORY_ID, "New Product", BigDecimal.valueOf(10.00));

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        ProductCreateRequest productCreateRequest = createProductCreateRequest(NON_EXISTING_CATEGORY_ID_STRING, "New Product", BigDecimal.valueOf(10.00));

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID_STRING), thrownException.getMessage());
    }

    @Test
    void addProduct_shouldThrowIllegalArgumentExceptionWhenCategoryStatusIsInactive() {

        ProductCreateRequest productCreateRequest = createProductCreateRequest(CATEGORY_ID_STRING, "New Product", BigDecimal.valueOf(10.00));

        Category category = createCategory(CATEGORY_ID, "Existing Category", CATEGORY_STATUS_INACTIVE, CREATED_AT_PAST, UPDATED_AT_PAST);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void updateProduct_shouldUpdateProductSuccessfullyWhenProductExistsAndIsNotSoldOut() {

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        Product updatedProduct = createProduct(existingProduct.getProductId(), updateRequest.getProductName(), updateRequest.getListPrice(), updateRequest.getCurrentPrice(), existingProduct.getProductStatus(), existingProduct.getAddedAt(), UPDATED_AT_NOW);

        ProductResponse productResponse = createProductResponse(updatedProduct.getProductId(), updatedProduct.getProductName(), updatedProduct.getListPrice(), updatedProduct.getCurrentPrice(), updatedProduct.getProductStatus(), updatedProduct.getAddedAt(), updatedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(existingProduct)).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(PRODUCT_ID_STRING, updateRequest);

        verify(productRepository, times(1)).findById(PRODUCT_ID);

        verify(productRepository, times(1)).saveAndFlush(productCaptor.capture());
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

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", null, null);

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        Product updatedProduct = createProduct(existingProduct.getProductId(), updateRequest.getProductName(), existingProduct.getListPrice(), existingProduct.getCurrentPrice(), existingProduct.getProductStatus(), existingProduct.getAddedAt(), UPDATED_AT_NOW);

        ProductResponse productResponse = createProductResponse(updatedProduct.getProductId(), updatedProduct.getProductName(), updatedProduct.getListPrice(), updatedProduct.getCurrentPrice(), updatedProduct.getProductStatus(), updatedProduct.getAddedAt(), updatedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(existingProduct)).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(PRODUCT_ID_STRING, updateRequest);

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, times(1)).saveAndFlush(productCaptor.capture());
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

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.updateProduct(NON_EXISTING_PRODUCT_ID_STRING, updateRequest));

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID_STRING), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIsSoldOut() {

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.SOLD_OUT, ADDED_AT_PAST, UPDATED_AT_PAST);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(PRODUCT_ID_STRING, updateRequest));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, is sold out and can not be updated.", PRODUCT_ID_STRING), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        assertThrows(IllegalArgumentException.class, () ->
                productService.updateProduct(INVALID_PRODUCT_ID, updateRequest));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void setProductStatus_shouldSetProductStatusSuccessfullyWhenProductExistsAndStatusIsDifferent() {

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        Product updatedProduct = createProduct(existingProduct.getProductId(), existingProduct.getProductName(), existingProduct.getListPrice(), existingProduct.getCurrentPrice(), PRODUCT_STATUS_OUT_OF_STOCK, existingProduct.getAddedAt(), UPDATED_AT_NOW);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for the product with id: %s.", PRODUCT_STATUS_OUT_OF_STOCK_STRING, PRODUCT_ID_STRING))
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(existingProduct)).thenReturn(updatedProduct);

        MessageResponse actualResponse = productService.setProductStatus(PRODUCT_ID_STRING, PRODUCT_STATUS_OUT_OF_STOCK_STRING);

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, times(1)).saveAndFlush(productCaptor.capture());
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
                productService.setProductStatus(INVALID_PRODUCT_ID, PRODUCT_STATUS_OUT_OF_STOCK_STRING));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        when(productRepository.findById(NON_EXISTING_PRODUCT_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.setProductStatus(NON_EXISTING_PRODUCT_ID_STRING, PRODUCT_STATUS_OUT_OF_STOCK_STRING));

        verify(productRepository, times(1)).findById(NON_EXISTING_PRODUCT_ID);
        verify(productRepository, never()).saveAndFlush(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", NON_EXISTING_PRODUCT_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductStatusIsInvalid() {

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), PRODUCT_STATUS_AVAILABLE, ADDED_AT_PAST, UPDATED_AT_PAST);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(PRODUCT_ID_STRING, INVALID_PRODUCT_STATUS));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductAlreadyHasTargetStatus() {

        Product existingProduct = createProduct(PRODUCT_ID, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), PRODUCT_STATUS_OUT_OF_STOCK, ADDED_AT_PAST, UPDATED_AT_PAST);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(PRODUCT_ID_STRING, PRODUCT_STATUS_OUT_OF_STOCK_STRING));

        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, never()).saveAndFlush(any(Product.class));

        assertEquals(String.format("Product with id: %s, already has status '%s'.", PRODUCT_ID_STRING, PRODUCT_STATUS_OUT_OF_STOCK_STRING.toUpperCase()), thrownException.getMessage());
    }
}