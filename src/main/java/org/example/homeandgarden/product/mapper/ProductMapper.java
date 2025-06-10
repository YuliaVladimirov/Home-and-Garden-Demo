package org.example.homeandgarden.product.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.product.dto.*;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.ProductProjection;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    public Product requestToProduct (
            ProductCreateRequest productCreateRequest,
            Category category){

        return Product.builder()
                .productName(productCreateRequest.getProductName())
                .description(productCreateRequest.getDescription())
                .listPrice(productCreateRequest.getListPrice())
                .currentPrice(productCreateRequest.getListPrice())
                .imageUrl(productCreateRequest.getImageUrl())
                .category(category)
                .build();
    }

    public ProductResponse productToResponse (Product product){

        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .listPrice(product.getListPrice())
                .currentPrice(product.getCurrentPrice())
                .productStatus(product.getProductStatus())
                .imageUrl(product.getImageUrl())
                .addedAt(product.getAddedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductProjectionResponse productProjectionToResponse(ProductProjection productProjection) {
        return ProductProjectionResponse.builder()
                .productId(productProjection.getProductId())
                .productName(productProjection.getProductName())
                .listPrice(productProjection.getListPrice())
                .currentPrice(productProjection.getCurrentPrice())
                .productStatus(productProjection.getProductStatus())
                .imageUrl(productProjection.getImageUrl())
                .addedAt(productProjection.getAddedAt())
                .updatedAt(productProjection.getUpdatedAt())
                .totalAmount(productProjection.getTotalAmount())
                .build();
    }

}
