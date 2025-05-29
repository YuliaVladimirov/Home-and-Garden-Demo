package org.example.homeandgarden.product.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.product.entity.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductProjection {

    private UUID productId;
    private String productName;
    private BigDecimal listPrice;
    private BigDecimal currentPrice;
    private ProductStatus productStatus;
    private String imageUrl;
    private Instant addedAt;
    private Instant updatedAt;
    private Long totalAmount;

}
