package org.example.homeandgarden.product.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.category.repository.CategoryRepository;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.product.dto.*;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.product.repository.ProductRepository;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public PagedModel<ProductResponse> getCategoryProducts(String categoryId, BigDecimal minPrice, BigDecimal maxPrice, Integer size, Integer page, String order, String sortBy) {

        UUID id = UUID.fromString(categoryId);
        Category existingCategory = categoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Category with id: %s, was not found.", categoryId)));

        if (existingCategory.getCategoryStatus().equals(CategoryStatus.INACTIVE)) {
            throw new IllegalArgumentException(String.format("Category with id: %s, is disabled.", categoryId));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        return new PagedModel<>(productRepository.findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(id, ProductStatus.AVAILABLE, minPrice, maxPrice, pageRequest).map(productMapper::productToResponse));
    }

    @Override
    public PagedModel<ProductResponse> getProductsByStatus(String productStatus, Integer size, Integer page, String order, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        if(productStatus == null) {
            return new PagedModel<>(productRepository.findAll(pageRequest).map(productMapper::productToResponse));
        } else {
            ProductStatus status = ProductStatus.valueOf(productStatus.toUpperCase());
            return new PagedModel<>(productRepository.findAllByProductStatus(status, pageRequest).map(productMapper::productToResponse));
        }
    }

    @Override
    public PagedModel<ProductProjectionResponse> getTopProducts(String status, Integer size, Integer page) {
        List<OrderStatus> statuses;
        if (status.equalsIgnoreCase("PAID")) {
            statuses = List.of(OrderStatus.PAID, OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED);
        } else {
            statuses = List.of(OrderStatus.CANCELED, OrderStatus.RETURNED);
        }
        return new PagedModel<>(productRepository.findTopProducts(statuses, PageRequest.of(page, size)).map(productMapper::productProjectionToResponse));
    }

    @Override
    public PagedModel<ProductProjectionResponse> getPendingProducts(String orderStatus, Integer days, Integer size, Integer page) {
        OrderStatus status = OrderStatus.valueOf(orderStatus.toUpperCase());
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return new PagedModel<>(productRepository.findPendingProducts(status, cutoff, PageRequest.of(page, size)).map(productMapper::productProjectionToResponse));
    }

    @Override
    public ProductProfitResponse getProfitByPeriod(String timeUnit, Integer timePeriod) {

        Instant cutoff = switch (timeUnit.toUpperCase()) {
            case "DAY" -> Instant.now().minus(timePeriod, ChronoUnit.DAYS);
            case "WEEK" -> Instant.now().minus((timePeriod * 7L), ChronoUnit.DAYS);
            case "MONTH" -> Instant.now().minus((timePeriod * 30L), ChronoUnit.DAYS);
            case "YEAR" -> Instant.now().minus((timePeriod * 365L), ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException(String.format("Unexpected value: %s", timeUnit));
        };

        BigDecimal profit = productRepository.findProfitByPeriod(OrderStatus.DELIVERED, cutoff);

        return ProductProfitResponse.builder()
                .timeUnit(timeUnit.toUpperCase())
                .timePeriod(timePeriod)
                .profit(profit)
                .build();
    }

    @Override
    public ProductResponse getProductById(String productId) {

        UUID id = UUID.fromString(productId);
        Product existingProduct = productRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Product with id: %s, was not found.", productId)));
        return productMapper.productToResponse(existingProduct);
    }

    @Override
    public ProductResponse addProduct(ProductCreateRequest productCreateRequest) {

        UUID id = UUID.fromString(productCreateRequest.getCategoryId());
        Category existingCategory = categoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Category with id: %s, was not found.", productCreateRequest.getCategoryId())));

        if (existingCategory.getCategoryStatus().equals(CategoryStatus.INACTIVE)) {
            throw new IllegalArgumentException(String.format("Category with id: %s, is disabled and no product can be added to this category.", productCreateRequest.getCategoryId()));
        }
        Product productToAdd = productMapper.requestToProduct(productCreateRequest, existingCategory);
        Product addedProduct = productRepository.saveAndFlush(productToAdd);
        return productMapper.productToResponse(addedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String productId, ProductUpdateRequest productUpdateRequest) {
        UUID id = UUID.fromString(productId);
        Product existingProduct = productRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Product with id: %s, was not found.", productId)));

        if (existingProduct.getProductStatus().equals(ProductStatus.SOLD_OUT)) {
            throw new IllegalArgumentException(String.format("Product with id: %s, is sold out and can not be updated.", productId));
        }

        Optional.ofNullable(productUpdateRequest.getProductName()).ifPresent(existingProduct::setProductName);
        Optional.ofNullable(productUpdateRequest.getDescription()).ifPresent(existingProduct::setDescription);
        Optional.ofNullable(productUpdateRequest.getListPrice()).ifPresent(existingProduct::setListPrice);
        Optional.ofNullable(productUpdateRequest.getCurrentPrice()).ifPresent(existingProduct::setCurrentPrice);
        Optional.ofNullable(productUpdateRequest.getImageUrl()).ifPresent(existingProduct::setImageUrl);

        Product updatedProduct = productRepository.saveAndFlush(existingProduct);
        return productMapper.productToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public MessageResponse setProductStatus(String productId, String productStatus) {

        UUID id = UUID.fromString(productId);
        Product existingProduct = productRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Product with id: %s, was not found.", productId)));

        ProductStatus status = ProductStatus.valueOf(productStatus.toUpperCase());

        if (existingProduct.getProductStatus().equals(status)) {
            throw new IllegalArgumentException(String.format("Product with id: %s, already has status '%s'.", productId, productStatus.toUpperCase()));
        }

        existingProduct.setProductStatus(status);
        Product updatedProduct = productRepository.saveAndFlush(existingProduct);

        if (!updatedProduct.getProductStatus().equals(ProductStatus.valueOf(productStatus))) {
            throw new IllegalStateException(String.format("Unfortunately something went wrong and status '%s' was not set for product with id: %s. Please, try again.", productStatus, productId));
        }

        return MessageResponse.builder()
                .message(String.format("Status '%s' was set for the product with id: %s.", productStatus, productId))
                .build();
    }
}