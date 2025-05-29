package org.example.homeandgarden.order.repository;

import org.example.homeandgarden.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, PagingAndSortingRepository<Order, UUID> {

    Page<Order> findByUserUserId(UUID userId, Pageable pageable);
    boolean existsByOrderId(UUID orderId);
}
