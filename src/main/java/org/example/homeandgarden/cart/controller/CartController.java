package org.example.homeandgarden.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cart controller", description = "Controller for managing user's cart")
public class CartController {

    private final CartService cartService;


    // 🔐 Self-access endpoints — available only to the authenticated user (operates on their own data)

    @Operation(summary = "Add an item to current user's shopping cart", description = "Adds a specified product with a given quantity to the shopping cart of the user currently authenticated in the system. The details are provided in the request body.")
    @ApiResponse(responseCode = "201", description = "Cart item successfully added.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me")
    public ResponseEntity<CartItemResponse> addCartItem(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @RequestBody
            @Valid
            CartItemCreateRequest cartItemCreateRequest) {

        String email = userDetails.getUsername();
        CartItemResponse response = cartService.addCartItem(email, cartItemCreateRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a item in the shopping cart of current user", description = "Modifies the details of an existing item in the shopping cart of the user currently authenticated in the system. The item is identified by its unique cart item Id.")
    @ApiResponse(responseCode = "200", description = "Cart item successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/{cartItemId}")
    public ResponseEntity<CartItemResponse> updateCartItem(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique cart item id (UUID)")
            String cartItemId,

            @RequestBody
            @Valid
            CartItemUpdateRequest cartItemUpdateRequest) {

        String email = userDetails.getUsername();
        CartItemResponse response = cartService.updateCartItem(email, cartItemId, cartItemUpdateRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Remove an item from the shopping cart of current user", description = "Deletes a specific item from the shopping cart of the user currently authenticated in the system. The item is identified by its unique cart item Id.")
    @ApiResponse(responseCode = "200", description = "Cart item successfully removed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/me/{cartItemId}")
    public ResponseEntity<MessageResponse> removeCarItem(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique cart item id (UUID)")
            String cartItemId) {

        String email = userDetails.getUsername();
        MessageResponse messageResponse = cartService.removeCarItem(email, cartItemId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}
