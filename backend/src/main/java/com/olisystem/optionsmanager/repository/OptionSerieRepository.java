package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.OptionSerie;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionSerieRepository extends JpaRepository<OptionSerie, UUID> {
  Optional<OptionSerie> findByCode(String code);
}
