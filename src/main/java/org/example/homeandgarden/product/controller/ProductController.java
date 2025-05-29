package org.example.homeandgarden.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.homeandgarden.product.dto.*;
import org.example.homeandgarden.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupThreeErrorResponses;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.swagger.GroupTwoErrorResponses;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product controller", description = "Controller for managing products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get products by status", description = "Provides functionality for getting products by status")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/status")
    public ResponseEntity<PagedModel<ProductResponse>> getProductsByStatus(

            @RequestParam(value = "productStatus", defaultValue = "AVAILABLE")
            @Pattern(regexp = "^(AVAILABLE|OUT_OF_STOCK|SOLD_OUT|available|out_of_stock|sold_out)$", message = "Invalid order orderStatus: Must be one of the: AVAILABLE, OUT_OF_STOCK or SOLD_OUT (available, out_of_stock or sold_out)")
            @Parameter(description = "Status of the product in the system")
            String productStatus,

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

            @RequestParam(value = "sortBy", defaultValue = "addedAt")
            @Pattern(regexp = "^(productName|listPrice|currentPrice|addedAt|updatedAt)$", message = "Invalid value: Must be one of the following: productName, listPrice, currentPrice, addedAt, updatedAt")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"productName", "listPrice", "currentPrice", "addedAt", "updatedAt"}))
            String sortBy) {

        PagedModel<ProductResponse> pageResponse = productService.getProductsByStatus(productStatus, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get best selling or most cancelled products", description = "Provides functionality for getting best selling or most cancelled products")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProjectionResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/top")
    public ResponseEntity<PagedModel<ProductProjectionResponse>> getTopProducts(

            @RequestParam(value = "status", defaultValue = "PAID")
            @Pattern(regexp = "^(PAID|CANCELED|paid|canceled)$", message = "Invalid status: Must be PAID or CANCELED (paid or canceled)")
            @Parameter(description = "Status:'paid' for best selling products, 'canceled' for most cancelled products", schema = @Schema(allowableValues = {"PAID", "CANCELED", "paid", "canceled"}))
            String status,

            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "Invalid parameter: Size must be greater than or equal to 1")
            @Parameter(description = "Number of elements per one page")
            Integer size,

            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "Invalid parameter: Page numeration starts from 0")
            @Parameter(description = "Page number to display")
            Integer page) {

        PagedModel<ProductProjectionResponse> pageResponse = productService.getTopProducts(status, size, page);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get products pending in certain order status", description = "Provides functionality for getting products that have been in certain order status for more than N days")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProjectionResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/pending")
    public ResponseEntity<PagedModel<ProductProjectionResponse>> getPendingProduct(

            @RequestParam(value = "orderStatus", defaultValue = "CREATED")
            @Pattern(regexp = "^(CREATED|PAID|ON_THE_WAY|created|paid|on_the_way)$", message = "Invalid order orderStatus: Must be CREATED, PAID or ON_THE_WAY (created, paid or on_the_way)")
            @Parameter(description = "Status of the order in which the product is pending", schema = @Schema(allowableValues = {"CREATED", "PAID", "ON_THE_WAY", "created", "paid", "on_the_way"}))
            String orderStatus,

            @RequestParam(value = "days", defaultValue = "10")
            @Positive(message = "Number of days must be a positive number")
            @Parameter(description = "Number of days for order status")
            Integer days,

            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "Invalid parameter: Size must be greater than or equal to 1")
            @Parameter(description = "Number of elements per one page")
            Integer size,

            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "Invalid parameter: Page numeration starts from 0")
            @Parameter(description = "Page number to display")
            Integer page) {

        PagedModel<ProductProjectionResponse> pageResponse = productService.getPendingProduct(orderStatus, days, size, page);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get profit for certain period ", description = "Provides functionality for getting profit for certain period (days, weeks, months, years)")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProfitResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/profit")
    public ResponseEntity<ProductProfitResponse> getProfitByPeriod(

            @RequestParam(value = "timeUnit", defaultValue = "DAY")
            @Pattern(regexp = "^(DAY|WEEK|MONTH|YEAR|day|week|month|year)$", message = "Invalid type of period: Must be DAY, WEEK, MONTH or YEAR (day,week, month or year)")
            @Parameter(description = "Time unit for profit calculating", schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH", "YEAR", "day", "week", "month", "year"}))
            String timeUnit,

            @RequestParam(value = "value", defaultValue = "10")
            @Positive(message = "Period must be a positive number")
            @Parameter(description = "Length of period for profit calculating")
            Integer value) {

        ProductProfitResponse productProfitResponse = productService.getProfitByPeriod(timeUnit, value);
        return new ResponseEntity<>(productProfitResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get product by id", description = "Provides functionality for getting a product from product catalog by id")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupThreeErrorResponses
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/{productId}")
    public ResponseEntity<ProductResponse> getProductById(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique product id (UUID)")
            String productId) {

        ProductResponse existingProduct = productService.getProductById(productId);
        return new ResponseEntity<>(existingProduct, HttpStatus.OK);
    }


    @Operation(summary = "Add a new product", description = "Provides functionality for adding a new product into product catalog")
    @ApiResponse(responseCode = "201", description = "CREATED", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(

            @RequestBody
            @Valid
            ProductCreateRequest productCreateRequest) {

        ProductResponse addedProduct = productService.addProduct(productCreateRequest);
        return new ResponseEntity<>(addedProduct, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a product", description = "Provides functionality for updating a product in product catalog")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique product id (UUID)")
            String productId,

            @RequestBody
            @Valid
            ProductUpdateRequest productUpdateRequest) {

        ProductResponse updatedProduct = productService.updateProduct(productId, productUpdateRequest);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @Operation(summary = "Set product status", description = "Provides functionality for setting status for a product")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{productId}/status")
    public ResponseEntity<MessageResponse> setProductStatus(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique product id (UUID)")
            String productId,

            @RequestParam(value = "productStatus", defaultValue = "AVAILABLE")
            @Pattern(regexp = "^(AVAILABLE|OUT_OF_STOCK|SOLD_OUT|available|out_of_stock|sold_out)$", message = "Invalid order orderStatus: Must be one of the: AVAILABLE, OUT_OF_STOCK or SOLD_OUT (available, out_of_stock or sold_out)")
            @Parameter(description = "Status of the product in the system", schema = @Schema(allowableValues = {"AVAILABLE", "OUT_OF_STOCK", "SOLD_OUT", "available", "out_of_stock", "sold_out"}))
            String productStatus) {

        MessageResponse message = productService.setProductStatus(productId, productStatus);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
