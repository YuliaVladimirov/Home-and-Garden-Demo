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
import org.springframework.data.domain.Page;
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


    // 🌐 Public access endpoints — no authentication required (accessible to all users)

    @Operation(summary = "Get product by its id", description = "Fetches the details of a single product using its unique identifier (UUID).")
    @ApiResponse(responseCode = "200", description = "Product successfully retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
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


    // 👮 Admin access endpoints — restricted to users with administrative privileges

    @Operation(summary = "Get products by status with pagination and sorting", description = "Retrieves a paginated and sortable list of products based on their status ('AVAILABLE', 'OUT_OF_STOCK' or 'SOLD_OUT'). Allows specifying page size, page number, sort order, and sort field.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved products, possibly an empty list if no matches.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/status")
    public ResponseEntity<Page<ProductResponse>> getProductsByStatus(

            @RequestParam(value = "productStatus", required = false)
            @Pattern(regexp = "^(AVAILABLE|OUT_OF_STOCK|SOLD_OUT|available|out_of_stock|sold_out)$", message = "Invalid order orderStatus: Must be one of the: 'AVAILABLE', 'OUT_OF_STOCK' or 'SOLD_OUT' ('available', 'out_of_stock' or 'sold_out')")
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
            @Pattern(regexp = "^(ASC|DESC|asc|desc)$", message = "Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")
            @Parameter(description = "Sort order: 'asc' for ascending, 'desc' for descending", schema = @Schema(allowableValues = {"ASC", "DESC", "asc", "desc"}))
            String order,

            @RequestParam(value = "sortBy", defaultValue = "addedAt")
            @Pattern(regexp = "^(productName|listPrice|currentPrice|addedAt|updatedAt)$", message = "Invalid value: Must be one of the following: 'productName', 'listPrice', 'currentPrice', 'addedAt', 'updatedAt'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"productName", "listPrice", "currentPrice", "addedAt", "updatedAt"}))
            String sortBy) {

        Page<ProductResponse> pageResponse = productService.getProductsByStatus(productStatus, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get best selling or most cancelled products by order status", description = "Fetches a paginated list of top products, categorized by their order status. This can be used to get either 'best selling' products (status=PAID) or 'most canceled' products (status=CANCELED).")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top products, possibly an empty list if no matches.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProjectionResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/top")
    public ResponseEntity<Page<ProductProjectionResponse>> getTopProducts(

            @RequestParam(value = "status", defaultValue = "PAID")
            @Pattern(regexp = "^(PAID|CANCELED|paid|canceled)$", message = "Invalid status: Must be 'PAID' or 'CANCELED' ('paid' or 'canceled')")
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

        Page<ProductProjectionResponse> pageResponse = productService.getTopProducts(status, size, page);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get products stuck in certain order status", description = "Fetches a paginated list of products that have been stuck in certain order status ('CREATED', 'PAID', 'ON_THE_WAY') for more than a given number of days.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved stuck products, possibly an empty list if no matches.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProjectionResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/pending")
    public ResponseEntity<Page<ProductProjectionResponse>> getPendingProducts(

            @RequestParam(value = "orderStatus", defaultValue = "CREATED")
            @Pattern(regexp = "^(CREATED|PAID|ON_THE_WAY|created|paid|on_the_way)$", message = "Invalid order orderStatus: Must be 'CREATED', 'PAID' or 'ON_THE_WAY' ('created', 'paid' or 'on_the_way')")
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

        Page<ProductProjectionResponse> pageResponse = productService.getPendingProducts(orderStatus, days, size, page);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Calculate profit for a specified period ", description = "Retrieves the aggregated profit for a defined time period. Allows specifying the unit of time ('DAY', 'WEEK', 'MONTH' or 'YEAR') and the duration (e.g., 7 days, 12 months).")
    @ApiResponse(responseCode = "200", description = "Profit successfully calculated and retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductProfitResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping(value = "/profit")
    public ResponseEntity<ProductProfitResponse> getProfitByPeriod(

            @RequestParam(value = "timeUnit", defaultValue = "DAY")
            @Pattern(regexp = "^(DAY|WEEK|MONTH|YEAR|day|week|month|year)$", message = "Invalid type of period: Must be 'DAY', 'WEEK', 'MONTH' or 'YEAR' ('day', 'week', 'month' or 'year')")
            @Parameter(description = "Time unit for profit calculating", schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH", "YEAR", "day", "week", "month", "year"}))
            String timeUnit,

            @RequestParam(value = "timePeriod", defaultValue = "10")
            @Positive(message = "Duration must be a positive number")
            @Parameter(description = "Time period for profit calculating")
            Integer timePeriod) {

        ProductProfitResponse productProfitResponse = productService.getProfitByPeriod(timeUnit, timePeriod);
        return new ResponseEntity<>(productProfitResponse, HttpStatus.OK);
    }

    @Operation(summary = "Add a new product", description = "Adds a new product into product catalog. The product details are provided in the request body.")
    @ApiResponse(responseCode = "201", description = "Product successfully added.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
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

    @Operation(summary = "Update an existing product", description = "Modifies an existing product identified by its unique Id. The details that need to be updated are provided in the request body.")
    @ApiResponse(responseCode = "200", description = "Product successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
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

    @Operation(summary = "Set product status ('AVAILABLE', 'OUT_OF_STOCK', or 'SOLD_OUT')", description = "Updates the availability status of a specific product identified by its unique Id. A product can be set to 'AVAILABLE', 'OUT_OF_STOCK', or 'SOLD_OUT'.")
    @ApiResponse(responseCode = "200", description = "Product status successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
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
            @Pattern(regexp = "^(AVAILABLE|OUT_OF_STOCK|SOLD_OUT|available|out_of_stock|sold_out)$", message = "Invalid order orderStatus: Must be one of the: 'AVAILABLE', 'OUT_OF_STOCK' or 'SOLD_OUT' ('available', 'out_of_stock' or 'sold_out')")
            @Parameter(description = "Status of the product in the system", schema = @Schema(allowableValues = {"AVAILABLE", "OUT_OF_STOCK", "SOLD_OUT", "available", "out_of_stock", "sold_out"}))
            String productStatus) {

        MessageResponse message = productService.setProductStatus(productId, productStatus);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
