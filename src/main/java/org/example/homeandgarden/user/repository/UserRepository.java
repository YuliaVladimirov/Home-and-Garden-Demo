package org.example.homeandgarden.user.repository;

import org.example.homeandgarden.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, PagingAndSortingRepository<User, UUID> {

    Page<User> findAllByIsEnabled(Boolean isEnabled, Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIsEnabledFalse(String email);
    boolean existsByEmailAndIsNonLockedFalse(String email);

    boolean existsByUserId(UUID userId);
    boolean existsByEmailAndRefreshToken(String email, String refreshToken);

    Optional<User> findByEmail(String email);
}