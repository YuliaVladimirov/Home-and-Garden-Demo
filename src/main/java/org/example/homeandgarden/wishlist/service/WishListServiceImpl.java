package org.example.homeandgarden.wishlist.service;

import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.mapper.WishListMapper;
import org.example.homeandgarden.wishlist.repository.WishListRepository;
import org.example.homeandgarden.product.repository.ProductRepository;
import org.example.homeandgarden.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishListServiceImpl implements WishListService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final WishListMapper wishListMapper;
    private final ProductMapper productMapper;

    @Override
    public PagedModel<WishListItemResponse> getUserWishListItems(String userId, Integer size, Integer page, String order) {

        UUID id = UUID.fromString(userId);
        if (!userRepository.existsByUserId(id)) {
            throw new DataNotFoundException(String.format("User with id: %s, was not found.", userId));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), "addedAt");
        Page<WishListItem> wishListPage = wishListRepository.findByUserUserId(id, pageRequest);

        return new PagedModel<>(wishListPage.map((item) -> wishListMapper.wishListItemToResponse(item,
                productMapper.productToResponse(item.getProduct()))));
    }


    @Override
    @Transactional
    public WishListItemResponse addWishListItem(WishListItemRequest wishListItemRequest) {
        UUID id = UUID.fromString(wishListItemRequest.getUserId());
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("User with id: %s, was not found.", wishListItemRequest.getUserId())));
        UUID productId = UUID.fromString(wishListItemRequest.getProductId());
        Product existingProduct = productRepository.findById(productId).orElseThrow(() -> new DataNotFoundException(String.format("Product with id: %s, was not found.", wishListItemRequest.getProductId())));

        if (!existingProduct.getProductStatus().equals(ProductStatus.AVAILABLE)) {
            throw new IllegalArgumentException(String.format("Product with id: %s has status '%s' and can not be added to the wish list.", existingProduct.getProductId(), existingProduct.getProductStatus().name()));
        }

        Set<WishListItem> wishList = existingUser.getWishList();
        for (WishListItem item : wishList) {
            if (item.getProduct().getProductId().equals(productId)) {
                throw new DataAlreadyExistsException(String.format("Product with id: %s is already in wish list.", wishListItemRequest.getProductId()));
            }
        }
        WishListItem wishListItemToAdd = wishListMapper.requestToWishListItem(existingUser, existingProduct);
        WishListItem addedWishListItem = wishListRepository.saveAndFlush(wishListItemToAdd);

        return wishListMapper.wishListItemToResponse(addedWishListItem, productMapper.productToResponse(addedWishListItem.getProduct()));
    }

    @Override
    @Transactional
    public MessageResponse removeWishListItem(String wishListItemId) {

        UUID id = UUID.fromString(wishListItemId);
        WishListItem existingWishListItem = wishListRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Wishlist item with id: %s, was not found.", wishListItemId)));

        wishListRepository.delete(existingWishListItem);

        return MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", wishListItemId))
                .build();
    }
}
