package org.example.homeandgarden.cart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.product.dto.ProductResponse;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying an cart item")
public class CartItemResponse {

    @JsonProperty("cartItemId")
    @Schema(description = "Unique cart item id (UUID)")
    private UUID cartItemId;

    @JsonProperty("quantity")
    @Schema(description = "Quantity of cart items in the cart")
    private Integer quantity;

    @JsonProperty("addedAt")
    @Schema(description = "Date the cart item was added to the cart")
    private Instant addedAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Date the cart item was last updated")
    private Instant updatedAt;

    @JsonProperty("product")
    @Schema(description = "Product related to this cart item")
    private ProductResponse product;

}
