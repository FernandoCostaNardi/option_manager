package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.AnalysisHouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisHouseRepository extends JpaRepository<AnalysisHouse, UUID> {
  Optional<AnalysisHouse> findByCnpj(String cnpj);

  @Query(
      "SELECT a FROM AnalysisHouse a WHERE "
          + "(:name IS NULL OR a.name LIKE %:name%) AND "
          + "(:cnpj IS NULL OR a.cnpj LIKE %:cnpj%)")
  Page<AnalysisHouse> findByFilters(
      @Param("name") String name, @Param("cnpj") String cnpj, Pageable pageable);
}
