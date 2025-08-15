package org.example.homeandgarden.wishlist.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.product.repository.ProductRepository;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.repository.UserRepository;
import org.example.homeandgarden.wishlist.dto.WishListItemRequest;
import org.example.homeandgarden.wishlist.dto.WishListItemResponse;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.example.homeandgarden.wishlist.mapper.WishListMapper;
import org.example.homeandgarden.wishlist.repository.WishListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
    public Page<WishListItemResponse> getUserWishListItems(String userId, Integer size, Integer page, String order) {

        UUID id = UUID.fromString(userId);
        if (!userRepository.existsByUserId(id)) {
            throw new DataNotFoundException(String.format("User with id: %s, was not found.", userId));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), "addedAt");
        Page<WishListItem> wishListPage = wishListRepository.findByUserUserId(id, pageRequest);

        return wishListPage.map((item) -> wishListMapper.wishListItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }

    @Override
    public Page<WishListItemResponse> getMyWishListItems(String email, Integer size, Integer page, String order) {

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        UUID id = existingUser.getUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), "addedAt");
        Page<WishListItem> wishListPage = wishListRepository.findByUserUserId(id, pageRequest);

        return wishListPage.map((item) -> wishListMapper.wishListItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }


    @Override
    @Transactional
    public WishListItemResponse addWishListItem(String email, WishListItemRequest wishListItemRequest) {

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        UUID productId = UUID.fromString(wishListItemRequest.getProductId());
        Product existingProduct = productRepository.findById(productId).orElseThrow(() -> new DataNotFoundException(String.format("Product with id: %s, was not found.", wishListItemRequest.getProductId())));

        if (!existingProduct.getProductStatus().equals(ProductStatus.AVAILABLE)) {
            throw new IllegalArgumentException(String.format("Product with id: %s has status '%s' and can not be added to the wish list.", existingProduct.getProductId(), existingProduct.getProductStatus().name()));
        }

        if (existingUser.getWishList().stream()
                .anyMatch(item -> item.getProduct().getProductId().equals(productId))) {
            throw new DataAlreadyExistsException(
                    String.format("Product with id: %s is already in wish list.", wishListItemRequest.getProductId())
            );
        }

        WishListItem wishListItemToAdd = wishListMapper.requestToWishListItem(existingUser, existingProduct);
        WishListItem addedWishListItem = wishListRepository.save(wishListItemToAdd);

        return wishListMapper.wishListItemToResponse(addedWishListItem, productMapper.productToResponse(addedWishListItem.getProduct()));
    }

    @Override
    @Transactional
    public MessageResponse removeWishListItem(String email, String wishListItemId) {

        UUID id = UUID.fromString(wishListItemId);
        WishListItem existingWishListItem = wishListRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Wishlist item with id: %s, was not found.", wishListItemId)));

        if (!existingWishListItem.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException(String.format("Wishlist item with id: %s, does not belong to the wishlist of the user with email: %s.", wishListItemId, email));
        }

        wishListRepository.delete(existingWishListItem);

        return MessageResponse.builder()
                .message(String.format("Wishlist item with id: %s, has been removed from wishlist.", wishListItemId))
                .build();
    }
}
