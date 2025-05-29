package org.example.homeandgarden.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying aggregation queries")
public class ProductProjectionResponse {

    @JsonProperty("productId")
    @Schema(description = "Unique product id (UUID)")
    private UUID productId;

    @JsonProperty("productName")
    @Schema(description = "Name of the product")
    private String productName;

    @JsonProperty("listPrice")
    @Schema(description = "Price of the product in the catalog")
    private BigDecimal listPrice;

    @JsonProperty("currentPrice")
    @Schema(description = "Current price of the product")
    private BigDecimal currentPrice;

    @JsonProperty("productStatus")
    @Schema(description = "Actual product status")
    private ProductStatus productStatus;

    @JsonProperty("imageUrl")
    @Schema(description = "Url of the image related to this product")
    private String imageUrl;

    @JsonProperty("addedAt")
    @Schema(description = "Date the product was added to the catalog")
    private Instant addedAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Date the product was last updated")
    private Instant updatedAt;

    @JsonProperty("totalAmount")
    @Schema(description = "Total amount of products")
    private Long totalAmount;
}
