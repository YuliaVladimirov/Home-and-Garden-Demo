package org.example.homeandgarden.wishlist.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.springframework.data.web.PagedModel;

public interface WishListService {

    PagedModel<WishListItemResponse> getUserWishListItems(String userId, Integer size, Integer page, String order);
    WishListItemResponse addWishListItem(WishListItemRequest wishListItemRequest);
    MessageResponse removeWishListItem(String wishListItemId);
}
