package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    List<User> findAllByRoleAndIsDeletedFalse(UserRole role);
}

