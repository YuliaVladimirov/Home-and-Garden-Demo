package org.example.homeandgarden.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.user.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_item_id")
    private UUID cartItemId;

    @Column(name = "quantity")
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "added_at")
    private Instant addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;
}

