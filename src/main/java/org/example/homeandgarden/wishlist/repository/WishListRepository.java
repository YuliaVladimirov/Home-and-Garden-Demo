package org.example.homeandgarden.wishlist.repository;

import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface WishListRepository extends JpaRepository<WishListItem, UUID>, PagingAndSortingRepository<WishListItem, UUID> {

    Page<WishListItem> findByUserUserId(UUID userId, Pageable pageable);

}
