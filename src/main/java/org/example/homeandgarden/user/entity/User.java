package org.example.homeandgarden.user.entity;

import lombok.*;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.order.entity.Order;
import jakarta.persistence.*;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.wishlist.entity.WishListItem;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole userRole;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_non_locked")
    @Builder.Default
    private Boolean isNonLocked = true;

    @CreationTimestamp
    @Column(name = "registered_at")
    private Instant registeredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WishListItem> wishList = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CartItem> cart = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Order> orders = new HashSet<>();
}
