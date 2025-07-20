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
@Schema(description = "Request for updating a product")
public class ProductUpdateRequest {

    @JsonProperty("productName")
    @Size(min = 2, max = 50, message = "Invalid name: Must be of 2 - 50 characters")
    @Schema(description = "Name of the product")
    private String productName;

    @JsonProperty("description")
    @Size(min = 2, max = 255, message = "Invalid description: Must be of 2 - 255 characters")
    @Schema(description = "Detailed description of the product")
    private String description;

    @JsonProperty("listPrice")
    @DecimalMin(value = "0.0", message = "List price must be non-negative")
    @DecimalMax(value = "999999.99", message = "List price must be less than or equal to 999999.99")
    @Digits(integer = 6, fraction = 2, message = "List price must have up to 6 digits and 2 decimal places")
    @Schema(description = "Price of the product in the catalog")
    private BigDecimal listPrice;

    @JsonProperty("currentPrice")
    @DecimalMin(value = "0.0", message = "Current price must be non-negative")
    @DecimalMax(value = "999999.99", message = "Current price must be less than or equal to 999999.99")
    @Digits(integer = 6, fraction = 2, message = "Current price must have up to 6 digits and 2 decimal places")
    @Schema(description = "Current price of the product")
    private BigDecimal currentPrice;

    @JsonProperty("imageUrl")
    @Pattern(regexp = "^https?://([-a-z0-9]{2,256}\\.){1,20}[a-z]{2,4}/[-a-zA-Z0-9_.#?&=%/]*$", message = "Invalid URL")
    @Schema(description = "Url of the image related to this product")
    private String imageUrl;

}
