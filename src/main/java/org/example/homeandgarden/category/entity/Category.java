package org.example.homeandgarden.category.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.product.entity.Product;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "categories")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_status")
    @Builder.Default
    private CategoryStatus categoryStatus = CategoryStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Product> products = new HashSet<>();
}