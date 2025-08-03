package org.example.homeandgarden.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.service.CartService;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.service.OrderService;
import org.example.homeandgarden.security.entity.UserDetailsImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.swagger.GroupTwoErrorResponses;
import org.example.homeandgarden.user.dto.*;
import org.example.homeandgarden.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.service.WishListService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User controller", description = "Controller fo managing user's accounts")
public class UserController {

    private final UserService userService;
    private final WishListService wishListService;
    private final CartService cartService;
    private final OrderService orderService;

    @Operation(summary = "Get users with filtering, pagination and sorting", description = "Fetches a paginated and sortable list of user accounts. Results can be filtered by their enabled status ('true' for active users, 'false' for disabled users).")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved users, possibly an empty list if no matches.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @GroupTwoErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsersByStatus(

            @RequestParam(value = "isEnabled", defaultValue = "true")
            @Parameter(description = "Enabled status: 'true' or 'false'", schema = @Schema(allowableValues = {"true", "false"}))
            Boolean isEnabled,

            @RequestParam(value = "isNonLocked", defaultValue = "true")
            @Parameter(description = "Non-locked status: 'true' or 'false'", schema = @Schema(allowableValues = {"true", "false"}))
            Boolean isNonLocked,

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

            @RequestParam(value = "sortBy", defaultValue = "registeredAt")
            @Pattern(regexp = "^(firstName|lastName|registeredAt|updatedAt)$", message = "Invalid value: Must be one of the following: 'firstName', 'lastName', 'registeredAt', 'updatedAt'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"firstName", "lastName", "registeredAt", "updatedAt"}))
            String sortBy) {

        Page<UserResponse> pageResponse = userService.getUsersByStatus(isEnabled, isNonLocked, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get user by its id", description = "Fetches the details of a single user account using their unique identifier (UUID).")
    @ApiResponse(responseCode = "200", description = "User successfully retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format in path variable")
            @Parameter(description = "Unique user id (UUID)")
            String userId) {

        UserResponse user = userService.getUserById(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Operation(summary = "Get current user's profile", description = "Fetches the profile details of the user currently authenticated in the system.")
    @ApiResponse(responseCode = "200", description = "User successfully retrieved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/profile")
    public ResponseEntity<UserResponse> getMyProfile(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails) {

        String email = userDetails.getUsername();
        UserResponse user = userService.getMyProfile(email);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Operation(summary = "Get user's wish list items", description = "Fetches a paginated and sortable list of wish list items for a specific user, identified by its unique Id.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved wish list items. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishListItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/{userId}/wishlist")
    public ResponseEntity<Page<WishListItemResponse>> getUserWishListItems(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId,

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
            String order) {

        Page<WishListItemResponse> pageResponse = wishListService.getUserWishListItems(userId, size, page, order);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get current user's wish list items", description = "Fetches a paginated and sortable list of wish list items for a user currently authenticated in the system.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved wish list items. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishListItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/wishlist")
    public ResponseEntity<Page<WishListItemResponse>> getMyWishListItems(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

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
            String order) {

        String email = userDetails.getUsername();
        Page<WishListItemResponse> pageResponse = wishListService.getMyWishListItems(email, size, page, order);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get user's cart items", description = "Fetches a paginated and sortable list of cart items for a specific user, identified by its unique Id.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved cart items. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/{userId}/cart")
    public ResponseEntity<Page<CartItemResponse>> getUserCartItems(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId,

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
            @Pattern(regexp = "^(addedAt|quantity)$", message = "Invalid value: Must be either: 'addedAt' or 'quantity'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"addedAt", "quantity"}))
            String sortBy) {

        Page<CartItemResponse> pageResponse = cartService.getUserCartItems(userId, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get current user's cart items", description = "Fetches a paginated and sortable list of cart items for a user currently authenticated in the system.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved cart items. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/cart")
    public ResponseEntity<Page<CartItemResponse>> getMyCartItems(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

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
            @Pattern(regexp = "^(addedAt|quantity)$", message = "Invalid value: Must be either: 'addedAt' or 'quantity'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"addedAt", "quantity"}))
            String sortBy) {

        String email = userDetails.getUsername();
        Page<CartItemResponse> pageResponse = cartService.getMyCartItems(email, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get user's orders", description = "Fetches a paginated and sortable list of orders for a specific user, identified by its unique Id.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved orders. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/{userId}/orders")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId,

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

            @RequestParam(value = "sortBy", defaultValue = "createdAt")
            @Pattern(regexp = "^(status|createdAt)$", message = "Invalid value: Must be either: 'orderStatus' or 'createdAt'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"status", "createdAt"}))
            String sortBy) {

        Page<OrderResponse> pageResponse = orderService.getUserOrders(userId, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get current user's orders", description = "Fetches a paginated and sortable list of orders for a user currently authenticated in the system.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved orders. Returns an empty page if the user has no items.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

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

            @RequestParam(value = "sortBy", defaultValue = "createdAt")
            @Pattern(regexp = "^(status|createdAt)$", message = "Invalid value: Must be either: 'orderStatus' or 'createdAt'")
            @Parameter(description = "The field the elements are sorted by", schema = @Schema(allowableValues = {"status", "createdAt"}))
            String sortBy) {

        String email = userDetails.getUsername();
        Page<OrderResponse> pageResponse = orderService.getMyOrders(email, size, page, order, sortBy);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Update an existing user", description = "Modifies an existing user account identified by their unique Id. The details that need to be updated are provided in the request body.")
    @ApiResponse(responseCode = "200", description = "User successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId,

            @RequestBody
            @Valid
            UserUpdateRequest userUpdateRequest) {

        UserResponse userResponse = userService.updateUser(userId, userUpdateRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @Operation(summary = "Assign a role to a user ('CLIENT' or 'ADMINISTRATOR')", description = "Updates the role of a specific user identified by their unique Id. A user can be set to 'CLIENT' or 'ADMINISTRATOR'.")
    @ApiResponse(responseCode = "200", description = "User role successfully updated.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<MessageResponse> setUserRole(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId,

            @RequestParam(value = "role")
            @Pattern(regexp = "^(CLIENT|ADMINISTRATOR|client|administrator)$", message = "Invalid order orderStatus: Must be one of the: 'CLIENT' or 'ADMINISTRATOR' ('client' or 'administrator')")
            @Parameter(description = "UserRole of the user in the system", schema = @Schema(allowableValues = {"CLIENT", "ADMINISTRATOR", "client", "administrator"}))
            String role) {

        MessageResponse messageResponse = userService.setUserRole(userId, role);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Toggle user account lock state", description = "Toggles the lock status of a specific user account identified by their unique Id. If the account is currently locked, it will be unlocked; if unlocked, it will be locked.")
    @ApiResponse(responseCode = "200", description = "User account lock state successfully toggled.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PatchMapping("/{userId}/toggle-lock")
    public ResponseEntity<MessageResponse> toggleLockState(

            @PathVariable
            @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", message = "Invalid UUID format")
            @Parameter(description = "Unique user id (UUID)")
            String userId) {

        MessageResponse messageResponse = userService.toggleLockState(userId);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Unregister a user", description = "Deactivates a user account in the system. The required confirmation should be provided in the request body")
    @ApiResponse(responseCode = "200", description = "User successfully unregistered.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/unregister")
    public ResponseEntity<MessageResponse> unregisterMyAccount(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @RequestBody
            @Valid
            UserUnregisterRequest userUnregisterRequest) {

        String email = userDetails.getUsername();
        MessageResponse messageResponse = userService.unregisterMyAccount(email, userUnregisterRequest);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Change user password", description = "Changes user password. The required confirmation should be provided in the request body")
    @ApiResponse(responseCode = "200", description = "User password successfully changed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/change-password")
    public ResponseEntity<MessageResponse> changeMyPassword(

            @AuthenticationPrincipal
            UserDetailsImpl userDetails,

            @RequestBody
            @Valid
            ChangePasswordRequest changePasswordRequest){

        String email = userDetails.getUsername();
        MessageResponse messageResponse = userService.changeMyPassword(email, changePasswordRequest);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }
}

