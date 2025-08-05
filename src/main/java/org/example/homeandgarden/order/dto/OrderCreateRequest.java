package org.example.homeandgarden.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for creating a new order")
public class OrderCreateRequest {

    @JsonProperty("firstName")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 30, message = "Invalid first name: Must be of 2 - 30 characters")
    @Schema(description = "First name of order recipient")
    private String firstName;

    @JsonProperty("lastName")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 30, message = "Invalid last name: Must be of 2 - 30 characters")
    @Schema(description = "Last name of order recipient")
    private String lastName;

    @JsonProperty("address")
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be at most 255 characters")
    @Schema(description = "Shipping address")
    private String address;

    @JsonProperty("zipCode")
    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "ZIP code must be exactly 5 digits")
    @Schema(description = "Shipping address ZIP code")
    private String zipCode;

    @JsonProperty("city")
    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "Invalid city: Must be of 2 - 100 characters")
    @Schema(description = "Delivery city")
    private String city;

    @JsonProperty("phone")
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+\\d{9,15}$", message = "Invalid phone number: Must be of 9 - 15 digits")
    @Schema(description = "Recipient phone number")
    private String phone;

    @JsonProperty("deliveryMethod")
    @NotBlank(message = "Delivery method is required")
    @Pattern(regexp = "^(COURIER_DELIVERY|CUSTOMER_PICKUP)$", message = "Invalid Delivery method: Must be one of: 'COURIER_DELIVERY' or 'CUSTOMER_PICKUP'")
    @Schema(description = "Order delivery method")
    private String deliveryMethod;

}

