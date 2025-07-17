package org.example.homeandgarden.cart.service;

import org.example.homeandgarden.cart.dto.CartItemCreateRequest;
import org.example.homeandgarden.cart.dto.CartItemUpdateRequest;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.cart.dto.CartItemResponse;
import org.example.homeandgarden.cart.mapper.CartMapper;
import org.example.homeandgarden.cart.repository.CartRepository;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.mapper.ProductMapper;
import org.example.homeandgarden.product.repository.ProductRepository;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Override
    public Page<CartItemResponse> getUserCartItems(String userId, Integer size, Integer page, String order, String sortBy) {
        UUID id = UUID.fromString(userId);
        if (!userRepository.existsByUserId(id)) {
            throw new DataNotFoundException(String.format("User with id: %s, was not found.", userId));
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<CartItem> cartPage = cartRepository.findByUserUserId(id, pageRequest);

        return cartPage.map((item) -> cartMapper.cartItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }

    @Override
    @Transactional
    public CartItemResponse addCartItem(CartItemCreateRequest cartItemCreateRequest) {

        UUID id = UUID.fromString(cartItemCreateRequest.getUserId());
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("User with id: %s, was not found.", cartItemCreateRequest.getUserId())));

        UUID productId = UUID.fromString(cartItemCreateRequest.getProductId());
        Product existingProduct = productRepository.findById(productId).orElseThrow(() -> new DataNotFoundException (String.format("Product with id: %s, was not found.", cartItemCreateRequest.getProductId())));

        if (!existingProduct.getProductStatus().equals(ProductStatus.AVAILABLE)) {
            throw new IllegalArgumentException(String.format("Product with id: %s has status '%s' and can not be added to the cart.", existingProduct.getProductId(), existingProduct.getProductStatus().name()));
        }

        Set<CartItem> cart = existingUser.getCart();
        for (CartItem item : cart) {
            if (item.getProduct().getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + cartItemCreateRequest.getQuantity());
                CartItem addedCartItem = cartRepository.saveAndFlush(item);
                return cartMapper.cartItemToResponse(addedCartItem, productMapper.productToResponse(addedCartItem.getProduct()));
            }
        }

        CartItem cartItemToAdd = cartMapper.requestToCartItem(cartItemCreateRequest, existingUser, existingProduct);
        CartItem addedCartItem = cartRepository.saveAndFlush(cartItemToAdd);

        return cartMapper.cartItemToResponse(addedCartItem, productMapper.productToResponse(addedCartItem.getProduct()));
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(String cartItemId, CartItemUpdateRequest cartItemUpdateRequest) {
        UUID id = UUID.fromString(cartItemId);
        CartItem existingCartItem = cartRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Cart item with id: %s, was not found.", cartItemId)));

        existingCartItem.setQuantity(cartItemUpdateRequest.getQuantity());

        CartItem updatedCartItem = cartRepository.saveAndFlush(existingCartItem);
        return cartMapper.cartItemToResponse(updatedCartItem, productMapper.productToResponse(updatedCartItem.getProduct()));
    }

    @Override
    @Transactional
    public MessageResponse removeCarItem(String cartItemId) {

        UUID id = UUID.fromString(cartItemId);
        CartItem existingCartItem = cartRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Cart item with id: %s, was not found.", cartItemId)));

        cartRepository.delete(existingCartItem);

        return MessageResponse.builder()
                .message(String.format("Cart item with id: %s, has been removed from cart.", cartItemId))
                .build();
    }
}
