package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.auth.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  // Change findByUsername to findByEmail
  Optional<User> findByEmail(String email);
}
