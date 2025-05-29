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
import org.springframework.data.web.PagedModel;
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

    @Operation(summary = "Get order items", description = "Provides functionality for getting order items by order id")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{orderId}/items")
    public ResponseEntity<PagedModel<OrderItemResponse>> getOrderItems(

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
            @Pattern(regexp = "^(ASC|DESC|asc|desc)$", message = "Invalid order: Must be ASC or DESC (asc or desc)")
            @Parameter(description = "Sort order: 'asc' for ascending, 'desc' for descending", schema = @Schema(allowableValues = {"ASC", "DESC", "asc", "desc"}))
            String order,

            @RequestParam(value = "sortBy", defaultValue = "priceAtPurchase")
            @Pattern(regexp = "^(quantity|priceAtPurchase)$", message = "Invalid value: Must be either: quantity or priceAtPurchase")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"quantity", "priceAtPurchase"}))
            String sortBy) {

        PagedModel<OrderItemResponse> pageResponse = orderItemService.getOrderItems(orderId, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }


    @Operation(summary = "Get order by id", description = "Provides functionality for getting user's order by order id")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
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

    @Operation(summary = "Get order status", description = "Provides functionality for getting current order status by order id")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
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


    @Operation(summary = "Add a new order", description = "Provides functionality for adding a new order")
    @ApiResponse(responseCode = "201", description = "CREATED", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
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

    @Operation(summary = "Update an order", description = "Provides functionality for updating an order")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
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

    @Operation(summary = "Cancel order", description = "Provides functionality for canceling an already placed order")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
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

    @Operation(summary = "Change order status ", description = "Provides functionality for changing the status of an already placed order")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{orderId}/status")
    public ResponseEntity<MessageResponse> setOrderStatus(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique order id (UUID)")
            String orderId) {

        MessageResponse messageResponse = orderService.setOrderStatus(orderId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}
