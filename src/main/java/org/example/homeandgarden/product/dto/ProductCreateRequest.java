package org.example.homeandgarden.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for adding a new product to the catalog")
public class ProductCreateRequest {

    @JsonProperty("categoryId")
    @NotBlank(message = "Category id is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
    @Schema(description = "Unique category id (UUID)")
    private String categoryId;

    @JsonProperty("productName")
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 50, message = "Invalid name: Must be of 2 - 50 characters")
    @Schema(description = "Name of the product")
    private String productName;

    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    @Size(min = 2, max = 255, message = "Invalid description: Must be of 2 - 255 characters")
    @Schema(description = "Detailed description of the product")
    private String description;

    @JsonProperty("listPrice")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00")
    @Digits(integer=6, fraction=2)
    @Schema(description = "Price of the product in the catalog")
    private BigDecimal listPrice;

    @JsonProperty("imageUrl")
    @NotBlank(message = "Image url is required")
    @Pattern(regexp = "^https?://([-a-z0-9]{2,256}\\.){1,20}[a-z]{2,4}/[-a-zA-Z0-9_.#?&=%/]*$", message = "Invalid URL")
    @Schema(description = "Url of the image related to this product")
    private String imageUrl;
}
