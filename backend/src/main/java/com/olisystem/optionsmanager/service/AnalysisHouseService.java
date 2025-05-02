package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.AnalysisHouse;
import com.olisystem.optionsmanager.repository.AnalysisHouseRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisHouseService {

  private final AnalysisHouseRepository analysisHouseRepository;

  public Page<AnalysisHouse> findAll(Pageable pageable, String name, String cnpj) {
    Specification<AnalysisHouse> spec = Specification.where(null);

    if (name != null && !name.isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
    }

    if (cnpj != null && !cnpj.isEmpty()) {
      spec = spec.and((root, query, cb) -> cb.like(root.get("cnpj"), "%" + cnpj + "%"));
    }

    return analysisHouseRepository.findAll(spec, pageable);
  }

  public Optional<AnalysisHouse> findById(UUID id) {
    return analysisHouseRepository.findById(id);
  }

  public Optional<AnalysisHouse> findByCnpj(String cnpj) {
    return analysisHouseRepository.findByCnpj(cnpj);
  }

  @Transactional
  public AnalysisHouse save(AnalysisHouse analysisHouse) {
    return analysisHouseRepository.save(analysisHouse);
  }

  @Transactional
  public void deleteById(UUID id) {
    analysisHouseRepository.deleteById(id);
  }
}
