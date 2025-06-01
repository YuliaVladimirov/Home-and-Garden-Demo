package org.example.homeandgarden.product.service;

import org.example.homeandgarden.product.dto.*;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;

public interface ProductService {

    PagedModel<ProductResponse> getCategoryProducts(String categoryId, BigDecimal minPrice, BigDecimal maxPrice, Integer size, Integer page, String order, String sortBy);
    PagedModel<ProductResponse> getProductsByStatus(String productStatus, Integer size, Integer page, String order, String sortBy);
    PagedModel<ProductProjectionResponse> getTopProducts(String status, Integer size, Integer page);
    PagedModel<ProductProjectionResponse> getPendingProduct(String status, Integer days, Integer size, Integer page);
    ProductProfitResponse getProfitByPeriod(String period, Integer timePeriod);
    ProductResponse getProductById(String productId);
    ProductResponse addProduct(ProductCreateRequest productCreateRequest);
    ProductResponse updateProduct(String productId, ProductUpdateRequest productUpdateRequest);
    MessageResponse setProductStatus(String productId, String productStatus);





}

