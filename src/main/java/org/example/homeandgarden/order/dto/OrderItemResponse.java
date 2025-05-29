package org.example.homeandgarden.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.product.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying an order item")
public class OrderItemResponse {

    @JsonProperty("orderItemId")
    @Schema(description = "Unique order item id (UUID)")
    private UUID orderItemId;

    @JsonProperty("quantity")
    @Schema(description = "Quantity of order items in the order")
    private Integer quantity;

    @JsonProperty("priceAtPurchase")
    @Schema(description = "Price the product was purchased")
    private BigDecimal priceAtPurchase;

    @JsonProperty("product")
    @Schema(description = "Product related to this order item")
    private ProductResponse product;

}
