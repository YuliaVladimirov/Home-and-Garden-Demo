package org.example.homeandgarden.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying an order")
public class OrderResponse {

    @JsonProperty("orderId")
    @Schema(description = "Unique order id (UUID)")
    private UUID orderId;

    @JsonProperty("firstName")
    @Schema(description = "First name of order recipient")
    private String firstName;

    @JsonProperty("lastName")
    @Schema(description = "Last name of order recipient")
    private String lastName;

    @JsonProperty("address")
    @Schema(description = "Shipping address")
    private String address;

    @JsonProperty("zipCode")
    @Schema(description = "Shipping address ZIP code")
    private String zipCode;

    @JsonProperty("city")
    @Schema(description = "Delivery city")
    private String city;

    @JsonProperty("phone")
    @Schema(description = "Recipient phone number")
    private String phone;

    @JsonProperty("deliveryMethod")
    @Schema(description = "Order delivery method")
    private DeliveryMethod deliveryMethod;

    @JsonProperty("orderStatus")
    @Schema(description = "Actual order status")
    private OrderStatus orderStatus;

    @JsonProperty("createdAt")
    @Schema(description = "Date the order was created")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Date the order was last updated")
    private Instant updatedAt;
}
