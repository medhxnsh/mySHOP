package com.myshop.repository.jpa;

import com.myshop.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository — Spring Data JPA repository for User entities.
 *
 * WHY DOES THIS INTERFACE WORK WITHOUT AN IMPLEMENTATION?
 * Spring Data JPA is magic (but explained!):
 * At application startup, Spring scans for interfaces extending JpaRepository.
 * For each one, it generates a PROXY CLASS at runtime that provides:
 * - All CRUD methods (save, findById, findAll, delete, etc.)
 * - Custom methods derived from method names (findByEmail → SELECT * FROM users
 * WHERE email=?)
 *
 * Method name conventions: findBy{Field}, existsBy{Field}, countBy{Field}
 * No SQL needed — Spring parses the method name and generates the query.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** SELECT * FROM users WHERE email = ? — used during login */
    Optional<User> findByEmail(String email);

    /** SELECT COUNT(*) > 0 FROM users WHERE email = ? — used during registration */
    boolean existsByEmail(String email);
}
