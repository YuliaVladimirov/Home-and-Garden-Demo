package org.example.homeandgarden.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "list_price", nullable = false)
    private BigDecimal listPrice;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    @Builder.Default
    private ProductStatus productStatus = ProductStatus.AVAILABLE;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

   @ManyToOne
   @JoinColumn(name = "category_id", nullable=false)
    private Category category;

    @OneToMany(mappedBy = "product",  fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WishListItem> wishListItems = new HashSet<>();

    @OneToMany(mappedBy = "product",  fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "product",  fetch = FetchType.LAZY)
    @Builder.Default
    private Set<OrderItem> orderItems = new HashSet<>();
}
