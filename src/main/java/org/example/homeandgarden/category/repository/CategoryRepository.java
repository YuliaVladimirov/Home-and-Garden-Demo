package org.example.homeandgarden.category.repository;

import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, PagingAndSortingRepository<Category, UUID> {

    boolean existsByCategoryName(String name);
    Page<Category> findAllByCategoryStatusIs (CategoryStatus status, Pageable pageable);

}
