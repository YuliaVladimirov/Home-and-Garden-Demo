package org.example.homeandgarden.order.repository;

import org.example.homeandgarden.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID>, PagingAndSortingRepository<OrderItem, UUID> {

    Page<OrderItem> findByOrderOrderId(UUID orderId, Pageable pageable);
}
