package org.example.homeandgarden.wishlist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homeandgarden.product.entity.Product;
import org.example.homeandgarden.user.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wish_list_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WishListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wish_list_item_id", updatable = false, nullable = false)
    private UUID wishListItemId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;
}
