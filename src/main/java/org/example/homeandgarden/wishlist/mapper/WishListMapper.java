package org.example.homeandgarden.wishlist.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishListMapper {

    public WishListItem requestToWishListItem (
            User user,
            Product product) {

        return WishListItem.builder()
                .user(user)
                .product(product)
                .build();
    }

    public WishListItemResponse wishListItemToResponse(
            WishListItem wishListItem,
            ProductResponse product) {

        return WishListItemResponse.builder()
                .wishListItemId(wishListItem.getWishListItemId())
                .addedAt(wishListItem.getAddedAt())
                .product(product)
                .build();
    }
}
