package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisHouseRepository
    extends JpaRepository<AnalysisHouse, UUID>, JpaSpecificationExecutor<AnalysisHouse> {
  // Buscar por CNPJ e usuário
  Optional<AnalysisHouse> findByCnpjAndUser(String cnpj, User user);

  // Buscar por nome e usuário
  Optional<AnalysisHouse> findByNameAndUser(String name, User user);

  // Buscar por ID e usuário
  Optional<AnalysisHouse> findByIdAndUser(UUID id, User user);

  // Buscar todas do usuário (com paginação)
  Page<AnalysisHouse> findByUser(User user, Pageable pageable);

  // Busca paginada por nome e usuário
  Page<AnalysisHouse> findByNameContainingIgnoreCaseAndUser(
      String name, User user, Pageable pageable);

  // Busca paginada por CNPJ e usuário
  Page<AnalysisHouse> findByCnpjContainingIgnoreCaseAndUser(
      String cnpj, User user, Pageable pageable);

  List<AnalysisHouse> findByUser(User user);
}
