package org.example.homeandgarden.cart.repository;

import org.example.homeandgarden.cart.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface CartRepository extends JpaRepository<CartItem, UUID>, PagingAndSortingRepository<CartItem, UUID> {

        Page<CartItem> findByUserUserId(UUID userId, Pageable pageable);

}
