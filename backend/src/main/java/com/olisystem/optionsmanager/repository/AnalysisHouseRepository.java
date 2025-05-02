package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.AnalysisHouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisHouseRepository
    extends JpaRepository<AnalysisHouse, UUID>, JpaSpecificationExecutor<AnalysisHouse> {
  Optional<AnalysisHouse> findByCnpj(String cnpj);
}
