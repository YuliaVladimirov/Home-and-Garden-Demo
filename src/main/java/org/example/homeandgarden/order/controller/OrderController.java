package org.example.homeandgarden.order.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.example.homeandgarden.order.dto.OrderCreateRequest;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.dto.OrderUpdateRequest;
import org.example.homeandgarden.order.service.OrderItemService;
import org.example.homeandgarden.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order controller", description = "Controller for managing user's orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @Operation(summary = "Get order items for a specific order", description = "Fetches a paginated and sortable list of order items within a given order, identified by its unique order Id.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved order items. Returns an empty page if the order has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{orderId}/items")
    public ResponseEntity<Page<OrderItemResponse>> getOrderItems(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId,

            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "Invalid parameter: Size must be greater than or equal to 1")
            @Parameter(description = "Number of elements per one page")
            Integer size,

            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "Invalid parameter: Page numeration starts from 0")
            @Parameter(description = "Page number to display")
            Integer page,

            @RequestParam(value = "order", defaultValue = "ASC")
            @Pattern(regexp = "^(ASC|DESC|asc|desc)$", message = "Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")
            @Parameter(description = "Sort order: 'asc' for ascending, 'desc' for descending", schema = @Schema(allowableValues = {"ASC", "DESC", "asc", "desc"}))
            String order,

            @RequestParam(value = "sortBy", defaultValue = "priceAtPurchase")
            @Pattern(regexp = "^(quantity|priceAtPurchase)$", message = "Invalid value: Must be either: 'quantity' or 'priceAtPurchase'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"quantity", "priceAtPurchase"}))
            String sortBy) {

        Page<OrderItemResponse> pageResponse = orderItemService.getOrderItems(orderId, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }


    @Operation(summary = "Get order by id", description = "Fetches the details of a single order using its unique identifier (UUID).")
    @ApiResponse(responseCode = "200", description = "Order successfully retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId) {

        OrderResponse orderResponse = orderService.getOrderById(orderId);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get the status of an order", description = "Fetches the current status of a specific order identified by its unique ID.")
    @ApiResponse(responseCode = "200", description = "Order status successfully retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{orderId}/status")
    public ResponseEntity<MessageResponse> getOrderStatus(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId) {

        MessageResponse messageResponse = orderService.getOrderStatus(orderId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }


    @Operation(summary = "Add a new order", description = "Initiates a new order in the system. The order details are provided in the request body.")
    @ApiResponse(responseCode = "201", description = "Order successfully added.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<OrderResponse> addOrder(

            @RequestBody
            @Valid
            OrderCreateRequest orderCreateRequest) {

        OrderResponse orderResponse = orderService.addOrder(orderCreateRequest);
        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing order", description = "Modifies an existing order identified by its unique Id. The details that need to be updated are provided in the request body.")
    @ApiResponse(responseCode = "200", description = "Order successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateOrder(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId,

            @RequestBody
            @Valid
            OrderUpdateRequest orderUpdateRequest) {

        OrderResponse orderResponse = orderService.updateOrder(orderId, orderUpdateRequest);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @Operation(summary = "Cancel an existing order", description = "Changes the status of a specific order to 'CANCELED', identified by its unique ID.")
    @ApiResponse(responseCode = "200", description = "Order successfully canceled.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasAnyRole('CLIENT')")
    @PatchMapping(value = "/{orderId}/cancel")
    public ResponseEntity<MessageResponse> cancelOrder(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId) {

        MessageResponse messageResponse = orderService.cancelOrder(orderId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Advance the status of an order", description = "Transitions a specific order, identified by its unique ID, to its next logical status within the order processing workflow (e.g., from 'CREATED' to 'PAID', or 'PAID' to 'ON_THE_WAY'). This endpoint does not take an explicit status parameter, instead inferring the next state.")
    @ApiResponse(responseCode = "200", description = "Order status successfully updated to the next stage.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{orderId}/status")
    public ResponseEntity<MessageResponse> toggleOrderStatus(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId) {

        MessageResponse messageResponse = orderService.toggleOrderStatus(orderId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}
