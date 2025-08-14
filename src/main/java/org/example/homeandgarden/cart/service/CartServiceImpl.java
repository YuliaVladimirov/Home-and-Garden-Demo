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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
    public Page<CartItemResponse> getMyCartItems(String email, Integer size, Integer page, String order, String sortBy) {

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        UUID id = existingUser.getUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<CartItem> cartPage = cartRepository.findByUserUserId(id, pageRequest);

        return cartPage.map((item) -> cartMapper.cartItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }

    @Override
    @Transactional
    public CartItemResponse addCartItem(String email, CartItemCreateRequest cartItemCreateRequest) {

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        UUID productId = UUID.fromString(cartItemCreateRequest.getProductId());
        Product existingProduct = productRepository.findById(productId).orElseThrow(() -> new DataNotFoundException (String.format("Product with id: %s, was not found.", cartItemCreateRequest.getProductId())));

        if (!existingProduct.getProductStatus().equals(ProductStatus.AVAILABLE)) {
            throw new IllegalArgumentException(String.format("Product with id: %s has status '%s' and can not be added to the cart.", existingProduct.getProductId(), existingProduct.getProductStatus().name()));
        }

        Optional<CartItem> optionalCartItem  = cartRepository.findByUserAndProduct(existingUser, existingProduct);

        CartItem addedCartItem;
        if (optionalCartItem.isPresent()) {
            CartItem existingCartItem = optionalCartItem.get();
            existingCartItem.setQuantity(existingCartItem.getQuantity() + cartItemCreateRequest.getQuantity());
            addedCartItem = cartRepository.save(existingCartItem);

        } else {
            CartItem cartItemToAdd = cartMapper.requestToCartItem(cartItemCreateRequest, existingUser, existingProduct);
            addedCartItem = cartRepository.save(cartItemToAdd);
        }

        return cartMapper.cartItemToResponse(addedCartItem, productMapper.productToResponse(addedCartItem.getProduct()));
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(String email, String cartItemId, CartItemUpdateRequest cartItemUpdateRequest) {

        UUID id = UUID.fromString(cartItemId);
        CartItem existingCartItem = cartRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Cart item with id: %s, was not found.", cartItemId)));

        if (!existingCartItem.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException(String.format("Cart item with id: %s, does not belong to the cart of the user with email: %s.", cartItemId, email));
        }

        existingCartItem.setQuantity(cartItemUpdateRequest.getQuantity());

        CartItem updatedCartItem = cartRepository.save(existingCartItem);
        return cartMapper.cartItemToResponse(updatedCartItem, productMapper.productToResponse(updatedCartItem.getProduct()));
    }

    @Override
    @Transactional
    public MessageResponse removeCarItem(String email, String cartItemId) {

        UUID id = UUID.fromString(cartItemId);
        CartItem existingCartItem = cartRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Cart item with id: %s, was not found.", cartItemId)));

        if (!existingCartItem.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException(String.format("Cart item with id: %s, does not belong to the cart of the user with email: %s.", cartItemId, email));
        }

        cartRepository.delete(existingCartItem);

        return MessageResponse.builder()
                .message(String.format("Cart item with id: %s, has been removed from cart.", cartItemId))
                .build();
    }
}
