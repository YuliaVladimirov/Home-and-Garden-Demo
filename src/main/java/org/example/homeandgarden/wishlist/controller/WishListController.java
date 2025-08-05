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
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.ErrorResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.service.WishListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/wishlist")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wishlist controller", description = "Controller for managing user's wishlist")
public class WishListController {

    private final WishListService wishListService;


    // 🔐 Self-access endpoints — available only to the authenticated user (operates on their own data)

    @Operation(summary = "Add a product to current user's wish list", description = "Adds a specified product to the wish list of the user currently authenticated in the system. The details are provided in the request body.")
    @ApiResponse(responseCode = "201", description = "Wish list item successfully added.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishListItemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflict: The product is already in the user's wish list.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me")
    public ResponseEntity<WishListItemResponse> addWishListItem(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @RequestBody
            @Valid
            WishListItemRequest wishListItemRequest) {

        String email = userDetails.getUsername();
        WishListItemResponse wishListItemResponse = wishListService.addWishListItem(email, wishListItemRequest);
        return new ResponseEntity<>(wishListItemResponse, HttpStatus.CREATED);
    }

    @Operation(summary = "Remove a product from current user's wish list", description = "Provides functionality for deleting a product from the wish list of the user currently authenticated in the system.")
    @ApiResponse(responseCode = "200", description = "Wish list item successfully removed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me/{wishListItemId}")
    public ResponseEntity<MessageResponse> removeWishListItem(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique wishlist item id (UUID)")
            String wishListItemId) {

        String email = userDetails.getUsername();
        MessageResponse messageResponse = wishListService.removeWishListItem(email, wishListItemId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}
