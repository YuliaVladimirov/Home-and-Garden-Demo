package org.example.homeandgarden.cart.service;

import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.domain.Page;

public interface CartService {

    Page<CartItemResponse> getUserCartItems(String userId, Integer size, Integer page, String order, String sortBy);
    Page<CartItemResponse> getMyCartItems(String email, Integer size, Integer page, String order, String sortBy);
    CartItemResponse addCartItem(String email, CartItemCreateRequest cartItemCreateRequest);
    CartItemResponse updateCartItem(String email, String cartItemId, CartItemUpdateRequest cartItemUpdateRequest);
    MessageResponse removeCarItem(String email, String cartItemId);
}
