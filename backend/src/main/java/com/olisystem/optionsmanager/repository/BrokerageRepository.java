package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerageRepository extends JpaRepository<Brokerage, UUID> {
  Optional<Brokerage> findByCnpj(String cnpj);

  Page<Brokerage> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Page<Brokerage> findByCnpjContainingIgnoreCase(String cnpj, Pageable pageable);

  Page<Brokerage> findByNameContainingIgnoreCaseAndCnpjContainingIgnoreCase(
      String name, String cnpj, Pageable pageable);

  Page<Brokerage> findByUser(User user, Pageable pageable);

  Page<Brokerage> findByNameContainingIgnoreCaseAndUser(String name, User user, Pageable pageable);

  Page<Brokerage> findByCnpjContainingIgnoreCaseAndUser(String cnpj, User user, Pageable pageable);

  Page<Brokerage> findByNameContainingIgnoreCaseAndCnpjContainingIgnoreCaseAndUser(
      String name, String cnpj, User user, Pageable pageable);

  // Buscar corretora por CNPJ e usuário
  Optional<Brokerage> findByCnpjAndUser(String cnpj, User user);

  // Buscar corretora por ID e usuário
  Optional<Brokerage> findByIdAndUser(UUID id, User user);
}
