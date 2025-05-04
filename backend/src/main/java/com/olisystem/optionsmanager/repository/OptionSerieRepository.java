package com.olisystem.optionsmanager.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.olisystem.optionsmanager.model.option_serie.OptionSerie;

public interface OptionSerieRepository extends JpaRepository<OptionSerie, UUID> {

  Optional<OptionSerie> findByCode(String code);
}
