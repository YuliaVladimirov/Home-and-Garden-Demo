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

    private ProductProjection createProductProjection(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt,  Long totalAmount) {
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

    private ProductProjectionResponse createProductProjectionResponse(UUID id, String productName, BigDecimal listPrice, BigDecimal currentPrice, ProductStatus productStatus, Instant addedAt, Instant updatedAt,  Long totalAmount) {
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

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        BigDecimal minPrice = BigDecimal.valueOf(10.0);
        BigDecimal maxPrice = BigDecimal.valueOf(50.0);

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product product1 = createProduct(UUID.randomUUID(), "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS));
        Product product2 = createProduct(UUID.randomUUID(), "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(3, ChronoUnit.DAYS));

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / size);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                id, ProductStatus.AVAILABLE, minPrice, maxPrice, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getCategoryProducts(categoryId, minPrice, maxPrice, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findById(id);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                id, ProductStatus.AVAILABLE, minPrice, maxPrice, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

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
    void getCategoryProducts_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString () {

        String invalidCategoryId = "INVALID_UUID";

        BigDecimal minPrice = BigDecimal.valueOf(10.0);
        BigDecimal maxPrice = BigDecimal.valueOf(50.0);

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        assertThrows(IllegalArgumentException.class, () ->
                productService.getCategoryProducts(invalidCategoryId, minPrice, maxPrice, size, page, order, sortBy));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(PageRequest.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getCategoryProducts_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingCategoryId = nonExistingId.toString();

        BigDecimal minPrice = BigDecimal.valueOf(10.0);
        BigDecimal maxPrice = BigDecimal.valueOf(50.0);

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        when(categoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
            productService.getCategoryProducts(nonExistingCategoryId, minPrice, maxPrice, size, page, order, sortBy));

        verify(categoryRepository, times(1)).findById(nonExistingId);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", nonExistingCategoryId), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldThrowIllegalArgumentExceptionWhenCategoryIsDisabled() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        BigDecimal minPrice = BigDecimal.valueOf(10.0);
        BigDecimal maxPrice = BigDecimal.valueOf(50.0);

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        Category disabledCategory = createCategory(id, "Disabled Category", CategoryStatus.INACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(2L, ChronoUnit.DAYS));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(disabledCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            productService.getCategoryProducts(categoryId, minPrice, maxPrice, size, page, order, sortBy));

        verify(categoryRepository, times(1)).findById(id);
        verify(productRepository, never()).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                any(UUID.class), any(ProductStatus.class), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, is disabled.", categoryId), thrownException.getMessage());
    }

    @Test
    void getCategoryProducts_shouldReturnEmptyPagedModelWhenNoProductsMatchCriteria() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        BigDecimal minPrice = BigDecimal.valueOf(200.00); // Price range that won't match
        BigDecimal maxPrice = BigDecimal.valueOf(300.00);

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        Category category = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<Product> emptyProductPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(id, ProductStatus.AVAILABLE, minPrice, maxPrice, pageRequest)).thenReturn(emptyProductPage);

        PagedModel<ProductResponse> actualResponse = productService.getCategoryProducts(categoryId, minPrice, maxPrice, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findById(id);
        verify(productRepository, times(1)).findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(
                id, ProductStatus.AVAILABLE, minPrice,maxPrice, pageRequest);
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getProductsByStatus_shouldRetrieveAndMapProductsByCertainStatusWithPagination() {

        ProductStatus status = ProductStatus.OUT_OF_STOCK;
        String categoryStatus = status.name();

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Product product1 = createProduct(UUID.randomUUID(), "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.OUT_OF_STOCK, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS));
        Product product2 = createProduct(UUID.randomUUID(), "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.OUT_OF_STOCK, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(3, ChronoUnit.DAYS));

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / size);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(productRepository.findAllByProductStatus(status, pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getProductsByStatus(categoryStatus, size, page, order, sortBy);

        verify(productRepository, times(1)).findAllByProductStatus(status, pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

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

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Product product1 = createProduct(UUID.randomUUID(), "Product One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS));
        Product product2 = createProduct(UUID.randomUUID(), "Product Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.OUT_OF_STOCK, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(3, ChronoUnit.DAYS));

        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());
        long expectedTotalPages = (long) Math.ceil((double) products.size() / size);

        ProductResponse productResponse1 = createProductResponse(product1.getProductId(), product1.getProductName(), product1.getListPrice(), product1.getCurrentPrice(), product1.getProductStatus(), product1.getAddedAt(), product1.getUpdatedAt());
        ProductResponse productResponse2 = createProductResponse(product2.getProductId(), product2.getProductName(), product2.getListPrice(), product2.getCurrentPrice(), product2.getProductStatus(), product2.getAddedAt(), product2.getUpdatedAt());

        when(productRepository.findAll(pageRequest)).thenReturn(productPage);
        when(productMapper.productToResponse(product1)).thenReturn(productResponse1);
        when(productMapper.productToResponse(product2)).thenReturn(productResponse2);

        PagedModel<ProductResponse> actualResponse = productService.getProductsByStatus(null, size, page, order, sortBy);

        verify(productRepository, times(1)).findAll(pageRequest);
        verify(productMapper, times(1)).productToResponse(product1);
        verify(productMapper, times(1)).productToResponse(product2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(products.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

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
    void getProductsByStatus_shouldThrowIllegalArgumentExceptionWhenProductStatusIsInvalid (){

        String invalidStatus = "INVALID_STATUS";

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "productName";

        assertThrows(IllegalArgumentException.class, () ->
                productService.getProductsByStatus(invalidStatus, size, page, order, sortBy));

        verify(productRepository, never()).findAllByProductStatus(any(ProductStatus.class), any(Pageable.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForPaidStatusWithPagination() {

        String status = "PAID";
        int page = 0;
        int size = 5;

        PageRequest pageRequest = PageRequest.of(page, size);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.PAID, OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS),  56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS),  39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / size);

        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts(status, size, page);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getTopProducts_shouldRetrieveAndMapTopProductsForCanceledStatusWithPagination() {

        String status = "CANCELED";
        int page = 0;
        int size = 5;

        PageRequest pageRequest = PageRequest.of(page, size);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.CANCELED, OrderStatus.RETURNED);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS),  56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS),  39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / size);


        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findTopProducts(eq(expectedStatuses), eq(pageRequest))).thenReturn(productProjectionPage).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts(status, size, page);

        verify(productRepository, times(1)).findTopProducts(eq(expectedStatuses), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getTopProducts_shouldReturnEmptyPagedModelIfNoTopProductsFound() {

        String status = "PAID";
        int size = 10;
        int page = 0;

        PageRequest pageRequest = PageRequest.of(page, size);
        List<OrderStatus> expectedStatuses = List.of(OrderStatus.PAID, OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED);
        Page<ProductProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(productRepository.findTopProducts(expectedStatuses, pageRequest)).thenReturn(emptyPage);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getTopProducts(status, size, page);

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

        OrderStatus status = OrderStatus.PAID;
        String orderStatus = status.name();

        int days = 7;
        int size = 2;
        int page = 0;

        PageRequest pageRequest = PageRequest.of(page, size);

        ProductProjection productProjection1 = createProductProjection(UUID.randomUUID(), "Projection One", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS),  56L);
        ProductProjection productProjection2 = createProductProjection(UUID.randomUUID(), "Projection Two", BigDecimal.valueOf(30.00), BigDecimal.valueOf(20.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS),  39L);

        List<ProductProjection> projections = List.of(productProjection1, productProjection2);
        Page<ProductProjection> productProjectionPage = new PageImpl<>(projections, pageRequest, projections.size());
        long expectedTotalPages = (long) Math.ceil((double) projections.size() / size);

        ProductProjectionResponse productProjectionResponse1 = createProductProjectionResponse(productProjection1.getProductId(), productProjection1.getProductName(), productProjection1.getListPrice(), productProjection1.getCurrentPrice(), productProjection1.getProductStatus(), productProjection1.getAddedAt(), productProjection1.getUpdatedAt(), productProjection1.getTotalAmount());
        ProductProjectionResponse productProjectionResponse2 = createProductProjectionResponse(productProjection2.getProductId(), productProjection2.getProductName(), productProjection2.getListPrice(), productProjection2.getCurrentPrice(), productProjection2.getProductStatus(), productProjection2.getAddedAt(), productProjection2.getUpdatedAt(), productProjection2.getTotalAmount());

        when(productRepository.findPendingProducts(eq(status), any(Instant.class), eq(pageRequest))).thenReturn(productProjectionPage);
        when(productMapper.productProjectionToResponse(productProjection1)).thenReturn(productProjectionResponse1);
        when(productMapper.productProjectionToResponse(productProjection2)).thenReturn(productProjectionResponse2);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatus, days, size, page);

        verify(productRepository, times(1)).findPendingProducts(eq(status), any(Instant.class), eq(pageRequest));
        verify(productMapper, times(1)).productProjectionToResponse(productProjection1);
        verify(productMapper, times(1)).productProjectionToResponse(productProjection2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(projections.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(projections.size(), actualResponse.getContent().size());
        assertTrue(actualResponse.getContent().contains(productProjectionResponse1));
        assertTrue(actualResponse.getContent().contains(productProjectionResponse2));
    }

    @Test
    void getPendingProducts_shouldReturnEmptyPagedModelIfNoPendingProductsMatch() {

        OrderStatus status = OrderStatus.PAID;
        String orderStatus = status.name();

        int days = 7;
        int size = 2;
        int page = 0;

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ProductProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(productRepository.findPendingProducts(eq(status), any(Instant.class), eq(pageRequest))).thenReturn(emptyPage);

        PagedModel<ProductProjectionResponse> actualResponse = productService.getPendingProducts(orderStatus, days, size, page);

        verify(productRepository, times(1)).findPendingProducts(eq(status), any(Instant.class), eq(pageRequest));
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
        Integer size = 5;
        Integer page = 0;

        assertThrows(IllegalArgumentException.class, () -> productService.getPendingProducts(invalidOrderStatus, days, size, page));

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

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        Product product = createProduct(id, "Existing Product", BigDecimal.valueOf(40.00), BigDecimal.valueOf(40.00), ProductStatus.AVAILABLE, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS));

        ProductResponse productResponse = createProductResponse(product.getProductId(), product.getProductName(), product.getListPrice(), product.getCurrentPrice(), product.getProductStatus(), product.getAddedAt(), product.getUpdatedAt());

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.productToResponse(product)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.getProductById(productId);

        verify(productRepository, times(1)).findById(id);
        verify(productMapper, times(1)).productToResponse(product);

        assertEquals(productResponse.getProductId(), actualResponse.getProductId());
        assertEquals(productResponse.getProductName(), actualResponse.getProductName());
        assertEquals(productResponse.getProductStatus(), actualResponse.getProductStatus());
    }

    @Test
    void getProductById_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingProductId = nonExistingId.toString();

        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
            productService.getProductById(nonExistingProductId));

        assertEquals(String.format("Product with id: %s, was not found.", nonExistingProductId), thrownException.getMessage());

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void getProductById_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        String invalidProductId = "NOT_A_VALID_UUID";

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(invalidProductId));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldAddProductSuccessfully() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        ProductCreateRequest productCreateRequest = createProductCreateRequest(categoryId, "New Product", BigDecimal.valueOf(10.00));

        Category category = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product productToSave = createProduct(null, productCreateRequest.getProductName(), productCreateRequest.getListPrice(), productCreateRequest.getListPrice(), ProductStatus.AVAILABLE, Instant.now(), Instant.now());

        Product savedProduct = createProduct(UUID.randomUUID(), productToSave.getProductName(), productToSave.getListPrice(), productToSave.getCurrentPrice(), productToSave.getProductStatus(), productToSave.getAddedAt(), productToSave.getUpdatedAt());

        ProductResponse productResponse = createProductResponse(savedProduct.getProductId(), savedProduct.getProductName(), savedProduct.getListPrice(), savedProduct.getCurrentPrice(), savedProduct.getProductStatus(), savedProduct.getAddedAt(), savedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productMapper.requestToProduct(productCreateRequest, category)).thenReturn(productToSave);
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(savedProduct);
        when(productMapper.productToResponse(savedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.addProduct(productCreateRequest);

        verify(categoryRepository, times(1)).findById(id);
        verify(productMapper, times(1)).requestToProduct(productCreateRequest, category);

        verify(productRepository, times(1)).saveAndFlush(productToSave);
        Product capturedProduct = productCaptor.getValue();
        assertNotNull(capturedProduct);
        assertEquals(productCreateRequest.getProductName(), capturedProduct.getProductName());
        assertEquals(productCreateRequest.getListPrice(), capturedProduct.getListPrice());
        assertEquals(ProductStatus.AVAILABLE, capturedProduct.getProductStatus());

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

        String invalidCategoryId = "INVALID_UUID";

        ProductCreateRequest productCreateRequest = createProductCreateRequest(invalidCategoryId, "New Product", BigDecimal.valueOf(10.00));

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void addProduct_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingCategoryId = nonExistingId.toString();

        ProductCreateRequest productCreateRequest = createProductCreateRequest(nonExistingCategoryId, "New Product", BigDecimal.valueOf(10.00));

        when(categoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(nonExistingId);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Category with id: %s, was not found.", nonExistingCategoryId), thrownException.getMessage());
    }

    @Test
    void addProduct_shouldThrowIllegalArgumentExceptionWhenCategoryStatusIsInactive() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        ProductCreateRequest productCreateRequest = createProductCreateRequest(categoryId, "New Product", BigDecimal.valueOf(10.00));

        Category category = createCategory(id, "Existing Category", CategoryStatus.INACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class, () -> productService.addProduct(productCreateRequest));

        verify(categoryRepository, times(1)).findById(id);
        verify(productMapper, never()).requestToProduct(any(ProductCreateRequest.class), any(Category.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void updateProduct_shouldUpdateProductSuccessfullyWhenProductExistsAndIsNotSoldOut() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product updatedProduct = createProduct(existingProduct.getProductId(), updateRequest.getProductName(), updateRequest.getListPrice(), updateRequest.getCurrentPrice(), existingProduct.getProductStatus(), existingProduct.getAddedAt(), Instant.now());

        ProductResponse productResponse = createProductResponse(updatedProduct.getProductId(), updatedProduct.getProductName(), updatedProduct.getListPrice(), updatedProduct.getCurrentPrice(),updatedProduct.getProductStatus(), updatedProduct.getAddedAt(), updatedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(productId, updateRequest);

        verify(productRepository, times(1)).findById(id);

        verify(productRepository, times(1)).saveAndFlush(existingProduct);
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

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", null, null);

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product updatedProduct = createProduct(existingProduct.getProductId(), updateRequest.getProductName(), existingProduct.getListPrice(), existingProduct.getCurrentPrice(), existingProduct.getProductStatus(), existingProduct.getAddedAt(), Instant.now());

        ProductResponse productResponse = createProductResponse(updatedProduct.getProductId(), updatedProduct.getProductName(), updatedProduct.getListPrice(), updatedProduct.getCurrentPrice(),updatedProduct.getProductStatus(), updatedProduct.getAddedAt(), updatedProduct.getUpdatedAt());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(updatedProduct);
        when(productMapper.productToResponse(updatedProduct)).thenReturn(productResponse);

        ProductResponse actualResponse = productService.updateProduct(productId, updateRequest);

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, times(1)).saveAndFlush(existingProduct);
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

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingProductId = nonExistingId.toString();

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
            productService.updateProduct(nonExistingProductId, updateRequest));

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", nonExistingProductId), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIsSoldOut() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.SOLD_OUT, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            productService.updateProduct(productId, updateRequest));

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));

        assertEquals(String.format("Product with id: %s, is sold out and can not be updated.", productId), thrownException.getMessage());
    }

    @Test
    void updateProduct_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        String invalidProductId = "INVALID_UUID";

        ProductUpdateRequest updateRequest = createProductUpdateRequest("Updated Name", BigDecimal.valueOf(15.00), BigDecimal.valueOf(10.00));

        assertThrows(IllegalArgumentException.class, () ->
            productService.updateProduct(invalidProductId, updateRequest));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(productMapper, never()).productToResponse(any(Product.class));
    }

    @Test
    void setProductStatus_shouldSetProductStatusSuccessfullyWhenProductExistsAndStatusIsDifferent() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        ProductStatus newStatus = ProductStatus.OUT_OF_STOCK;
        String productNewStatus = newStatus.toString();

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product updatedProduct = createProduct(existingProduct.getProductId(), existingProduct.getProductName(), existingProduct.getListPrice(), existingProduct.getCurrentPrice(), newStatus, existingProduct.getAddedAt(), Instant.now());

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for the product with id: %s.", productNewStatus, productId))
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(updatedProduct);

        MessageResponse actualResponse = productService.setProductStatus(productId, productNewStatus);

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, times(1)).saveAndFlush(existingProduct);
        Product capturedProduct = productCaptor.getValue();
        assertEquals(existingProduct.getProductId(), capturedProduct.getProductId());
        assertEquals(existingProduct.getProductName(), capturedProduct.getProductName());
        assertEquals(newStatus, capturedProduct.getProductStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductIdIsInvalidUuidString() {

        String invalidProductId = "INVALID_UUID";

        ProductStatus newStatus = ProductStatus.OUT_OF_STOCK;
        String productNewStatus = newStatus.toString();

        assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(invalidProductId, productNewStatus));

        verify(productRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowDataNotFoundExceptionWhenProductDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingProductId = nonExistingId.toString();

        String newStatus = ProductStatus.OUT_OF_STOCK.name();

        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                productService.setProductStatus(nonExistingProductId, newStatus));

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));

        assertEquals(String.format("Product with id: %s, was not found.", nonExistingProductId), thrownException.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductStatusIsInvalid() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        String invalidStatus = "INVALID_STATUS";

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(productId, invalidStatus));

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void setProductStatus_shouldThrowIllegalArgumentExceptionWhenProductAlreadyHasTargetStatus() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        String newStatus = ProductStatus.OUT_OF_STOCK.name();

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.OUT_OF_STOCK, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                productService.setProductStatus(productId, newStatus));

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, never()).saveAndFlush(any(Product.class));

        assertEquals(String.format("Product with id: %s, already has status '%s'.", productId, newStatus.toUpperCase()), thrownException.getMessage());
    }

    @Test
    void setProductStatus_shouldThrowIllegalStateExceptionWhenStatusUpdateFailsOnSave() {

        UUID id = UUID.randomUUID();
        String productId = id.toString();

        ProductStatus newStatus = ProductStatus.OUT_OF_STOCK;
        String productNewStatus = newStatus.name();

        Product existingProduct = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Product productWithOriginalStatus = createProduct(id, "Original Name", BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), ProductStatus.AVAILABLE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(productWithOriginalStatus);

        IllegalStateException thrownException = assertThrows(IllegalStateException.class, () ->
                productService.setProductStatus(productId, productNewStatus));

        verify(productRepository, times(1)).findById(id);
        verify(productRepository, times(1)).saveAndFlush(existingProduct);
        Product capturedProduct = productCaptor.getValue();
        assertNotNull(capturedProduct);
        assertEquals(existingProduct.getProductId(), capturedProduct.getProductId());
        assertEquals(existingProduct.getProductName(), capturedProduct.getProductName());
        assertEquals(newStatus, capturedProduct.getProductStatus());

        assertEquals(String.format("Unfortunately something went wrong and status '%s' was not set for product with id: %s. Please, try again.", productNewStatus, productId), thrownException.getMessage());
    }
}