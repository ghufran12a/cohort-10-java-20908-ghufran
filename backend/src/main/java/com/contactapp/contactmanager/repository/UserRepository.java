package com.contactapp.contactmanager.repository;

import com.contactapp.contactmanager.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for users. We write NO SQL and NO implementation:
 * Spring Data JPA builds the queries from the method names.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // SELECT * FROM users WHERE phone = ?
    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
