package org.example.homeandgarden.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.category.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.service.ProductService;
import org.example.homeandgarden.shared.ErrorResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupFourErrorResponses;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.swagger.GroupTwoErrorResponses;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Category controller", description = "Controller for managing product's category")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @Operation(summary = "Get all categories", description = "Provides functionality for getting all categories")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<PagedModel<CategoryResponse>> getAllCategories(

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

            @RequestParam(value = "sortBy", defaultValue = "createdAt")
            @Pattern(regexp = "^(categoryName|createdAt|updatedAt)$", message = "Invalid value: Must be one of the following: categoryName, createdAt, updatedAt")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"categoryName", "createdAt", "updatedAt"}))
            String sortBy) {

        PagedModel<CategoryResponse> pageResponse = categoryService.getAllCategories(size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get categories by status", description = "Provides functionality for getting categories by status")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/status")
    public ResponseEntity<PagedModel<CategoryResponse>> getCategoriesByStatus(

            @RequestParam(value = "categoryStatus", defaultValue = "ACTIVE")
            @Pattern(regexp = "^(ACTIVE|INACTIVE|active|inactive)$", message = "Invalid order orderStatus: Must be one of the: ACTIVE or INACTIVE(active or inactive)")
            @Parameter(description = "Status of the category in the system")
            String categoryStatus,

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

            @RequestParam(value = "sortBy", defaultValue = "createdAt")
            @Pattern(regexp = "^(categoryName|createdAt|updatedAt)$", message = "Invalid value: Must be one of the following: categoryName, createdAt, updatedAt")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"categoryName", "createdAt", "updatedAt"}))
            String sortBy) {

        PagedModel<CategoryResponse> pageResponse = categoryService.getCategoriesByStatus(categoryStatus, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get category products", description = "Provides functionality for getting all products of certain category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<PagedModel<ProductResponse>> getCategoryProducts(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique category id (UUID)")
            String categoryId,

            @RequestParam(value = "minPrice",defaultValue = "0.0")
            @DecimalMin(value = "0.0")
            @Digits(integer = 6, fraction = 2)
            @Parameter(description = "Minimal price for the filter range")
            BigDecimal minPrice,

            @RequestParam(value = "maxPrice",defaultValue = "999999.0")
            @DecimalMax(value = "999999.0")
            @Digits(integer = 6, fraction = 2)
            @Parameter(description = "Maximal price for the filter range")
            BigDecimal maxPrice,

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

        PagedModel<ProductResponse> pageResponse = productService.getCategoryProducts(categoryId, minPrice, maxPrice, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Create a category", description = "Provides functionality for creating a new product category")
    @ApiResponse(responseCode = "201", description = "CREATED", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    @ApiResponse(responseCode = "409", description = "CONFLICT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping
    public ResponseEntity <CategoryResponse> addCategory(

            @RequestBody
            @Valid
            CategoryRequest categoryRequest) {

        CategoryResponse addedCategory = categoryService.addCategory(categoryRequest);
        return new ResponseEntity<>(addedCategory, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a category", description = "Provides functionality for updating certain product category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{categoryId}")
    public ResponseEntity <CategoryResponse> updateCategory(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique category id (UUID)")
            String categoryId,

            @RequestBody
            @Valid
            CategoryRequest categoryRequest) {

        CategoryResponse updatedCategory = categoryService.updateCategory(categoryId, categoryRequest);
        return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
    }

    @Operation(summary = "Set category status", description = "Provides functionality for setting status for a category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping(value = "/{categoryId}/status")
    public ResponseEntity<MessageResponse> setCategoryStatus(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique category id (UUID)")
            String categoryId,

            @RequestParam(value = "categoryStatus", defaultValue = "ACTIVE")
            @Pattern(regexp = "^(ACTIVE|INACTIVE|active|inactive)$", message = "Invalid order orderStatus: Must be one of the: ACTIVE or INACTIVE(active or inactive)")
            @Parameter(description = "Status of the category in the system", schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE", "active", "inactive"}))
            String categoryStatus) {

        MessageResponse messageResponse = categoryService.setCategoryStatus(categoryId, categoryStatus);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}