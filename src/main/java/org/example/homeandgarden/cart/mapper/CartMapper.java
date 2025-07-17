package org.example.homeandgarden.cart.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.user.entity.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartMapper {

    public CartItem requestToCartItem(
            CartItemCreateRequest cartItemCreateRequest,
            User user,
            Product product) {

      return CartItem.builder()
              .quantity(cartItemCreateRequest.getQuantity())
              .user(user)
              .product(product)
              .build();
    }

    public CartItemResponse cartItemToResponse(
            CartItem cartItem,
            ProductResponse productResponse) {

        return CartItemResponse.builder()
                .cartItemId(cartItem.getCartItemId())
                .quantity(cartItem.getQuantity())
                .addedAt(cartItem.getAddedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .product(productResponse)
                .build();
    }
}
