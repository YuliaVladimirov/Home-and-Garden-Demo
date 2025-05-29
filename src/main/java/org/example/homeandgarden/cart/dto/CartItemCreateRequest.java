package org.example.homeandgarden.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for creating a new cart item")
public class CartItemCreateRequest {

    @JsonProperty("userId")
    @NotBlank(message = "User id is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
    @Schema(description = "Unique user id (UUID)")
    private String userId;

    @JsonProperty("productId")
    @NotBlank(message = "Product id is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
    @Schema(description = "Unique product id (UUID)")
    private String productId;

    @JsonProperty("quantity")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Quantity cannot exceed 1000")
    @Schema(description = "Quantity of cart items in the cart")
    private Integer quantity;
}
