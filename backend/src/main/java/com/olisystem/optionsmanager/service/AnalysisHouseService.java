package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.AnalysisHouse;
import com.olisystem.optionsmanager.repository.AnalysisHouseRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisHouseService {

  private final AnalysisHouseRepository analysisHouseRepository;

  public Page<AnalysisHouse> findAll(Pageable pageable, String name, String cnpj) {
    return analysisHouseRepository.findByFilters(name, cnpj, pageable);
  }

  public Optional<AnalysisHouse> findById(UUID id) {
    return analysisHouseRepository.findById(id);
  }

  public Optional<AnalysisHouse> findByCnpj(String cnpj) {
    return analysisHouseRepository.findByCnpj(cnpj);
  }

  public AnalysisHouse save(AnalysisHouse analysisHouse) {
    return analysisHouseRepository.save(analysisHouse);
  }

  public void deleteById(UUID id) {
    analysisHouseRepository.deleteById(id);
  }
}
