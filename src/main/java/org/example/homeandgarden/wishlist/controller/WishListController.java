package org.example.homeandgarden.wishlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.shared.ErrorResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.service.WishListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/wishlist")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wishlist controller", description = "Controller for managing user's wishlist")
public class WishListController {

    private final WishListService wishListService;


    @Operation(summary = "Add a new wishlist item", description = "Provides functionality for adding a new product into user's wishlist")
    @ApiResponse(responseCode = "201", description = "CREATED", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishListItemResponse.class)))
    @ApiResponse(responseCode = "409", description = "CONFLICT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<WishListItemResponse> addWishListItem(

            @RequestBody
            @Valid
            WishListItemRequest wishListItemRequest) {

        WishListItemResponse wishListItemResponse = wishListService.addWishListItem(wishListItemRequest);
        return new ResponseEntity<>(wishListItemResponse, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete a wishlist item", description = "Provides functionality for deleting a product from user's wish list")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('CLIENT')")
    @DeleteMapping("/{wishListItemId}")
    public ResponseEntity<MessageResponse> removeWishListItem(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique wishlist item id (UUID)")
            String wishListItemId) {

        MessageResponse messageResponse = wishListService.removeWishListItem(wishListItemId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}
