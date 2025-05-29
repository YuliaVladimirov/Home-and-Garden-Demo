package org.example.homeandgarden.wishlist.dto;

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
@Schema(description = "Response for displaying a wishlist item")
public class WishListItemResponse {

    @JsonProperty("wishListItemId")
    @Schema(description = "Unique wishlist item id (UUID)")
    private UUID wishListItemId;

    @JsonProperty("addedAt")
    @Schema(description = "Date the wishlist item was added")
    private Instant addedAt;

    @JsonProperty("product")
    @Schema(description = "Date the wishlist item was last updated")
    private ProductResponse product;

}
