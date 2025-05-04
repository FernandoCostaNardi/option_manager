package com.olisystem.optionsmanager.repository;

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
}
