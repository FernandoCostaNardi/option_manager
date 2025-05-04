package com.olisystem.optionsmanager.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.olisystem.optionsmanager.model.auth.User;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  // Change findByUsername to findByEmail
  Optional<User> findByEmail(String email);
}
