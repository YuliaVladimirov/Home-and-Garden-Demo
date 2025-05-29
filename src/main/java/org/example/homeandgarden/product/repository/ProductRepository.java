package org.example.homeandgarden.product.repository;

import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.product.entity.ProductProjection;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, PagingAndSortingRepository<Product, UUID> {

    Page<Product> findAllByCategoryCategoryIdAndProductStatusIsAndCurrentPriceGreaterThanAndCurrentPriceLessThan(UUID categoryId, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> findAllByProductStatus(ProductStatus status, Pageable pageable);

    @Query("""
            SELECT new org.example.homeandgarden.product.entity.ProductProjection(
            product.productId,
            product.productName,
            product.listPrice,
            product.currentPrice,
            product.productStatus,
            product.imageUrl,
            product.addedAt,
            product.updatedAt,
            SUM(orderItem.quantity)
            )
            FROM OrderItem orderItem
            JOIN orderItem.product product
            JOIN orderItem.order order
            WHERE order.orderStatus IN :statuses
            GROUP BY
            product.productId,
            product.productName,
            product.listPrice,
            product.currentPrice,
            product.productStatus,
            product.imageUrl,
            product.addedAt,
            product.updatedAt
            ORDER BY SUM(orderItem.quantity) DESC
            """)
    Page<ProductProjection> findTopProducts(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);


    @Query("""
            SELECT  new org.example.homeandgarden.product.entity.ProductProjection(
            product.productId,
            product.productName,
            product.listPrice,
            product.currentPrice,
            product.productStatus,
            product.imageUrl,
            product.addedAt,
            product.updatedAt,
            SUM(orderItem.quantity)
            )
            FROM OrderItem orderItem
            JOIN orderItem.product product
            JOIN orderItem.order order
            WHERE order.orderStatus = :status and order.createdAt < :cutoff
            GROUP BY
            product.productId,
            product.productName,
            product.listPrice,
            product.currentPrice,
            product.productStatus,
            product.imageUrl,
            product.addedAt,
            product.updatedAt
            ORDER BY SUM(orderItem.quantity) DESC
            """)
    Page<ProductProjection> findPendingProducts(@Param("status") OrderStatus status, @Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("""
            SELECT SUM(orderItem.quantity*orderItem.priceAtPurchase)
            FROM OrderItem orderItem
            JOIN orderItem.product product
            JOIN orderItem.order order
            WHERE order.orderStatus = :status and order.createdAt >= :cutoff
            """)
    BigDecimal findProfitByPeriod(@Param("status") OrderStatus status, @Param("cutoff") Instant cutoff);
}