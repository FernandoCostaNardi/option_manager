package com.olisystem.optionsmanager.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;

@Repository
public interface AnalysisHouseRepository
    extends JpaRepository<AnalysisHouse, UUID>, JpaSpecificationExecutor<AnalysisHouse> {
  Optional<AnalysisHouse> findByCnpj(String cnpj);
}
