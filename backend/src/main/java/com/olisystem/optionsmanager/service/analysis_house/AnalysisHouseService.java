package com.olisystem.optionsmanager.service.analysis_house;

import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseCreateRequestDto;
import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseResponseDto;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.mapper.analysis_house.AnalysisHouseMapper;
import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.repository.AnalysisHouseRepository;
import com.olisystem.optionsmanager.util.SecurityUtil;
import com.olisystem.optionsmanager.util.UuidUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AnalysisHouseService {

  @Autowired private AnalysisHouseRepository analysisHouseRepository;

  public List<AnalysisHouse> findAll() {
    return analysisHouseRepository.findAll();
  }

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

  public AnalysisHouse save(AnalysisHouse analysisHouse) {
    return analysisHouseRepository.save(analysisHouse);
  }

  public void deleteById(UUID id) {
    analysisHouseRepository.deleteById(id);
  }

  public void deleteById(String id) {
    UUID analysisHouseId = UuidUtil.parseUuid(id);
    deleteById(analysisHouseId);
  }

  public AnalysisHouseResponseDto createAnalysisHouse(AnalysisHouseCreateRequestDto dto) {
    User user = SecurityUtil.getLoggedUser();
    AnalysisHouse analysisHouse = AnalysisHouseMapper.toEntity(dto, user);

    return findByCnpj(analysisHouse.getCnpj())
        .map(
            existing -> {
              log.warn(
                  "Tentativa de criar casa de análise com CNPJ duplicado: {}",
                  analysisHouse.getCnpj());
              return AnalysisHouseMapper.toDto(existing);
            })
        .orElseGet(
            () -> {
              AnalysisHouse savedAnalysisHouse = save(analysisHouse);
              return AnalysisHouseMapper.toDto(savedAnalysisHouse);
            });
  }

  public AnalysisHouseResponseDto updateAnalysisHouse(
      String id, AnalysisHouseCreateRequestDto dto) {
    UUID analysisHouseId = UuidUtil.parseUuid(id);
    User user = SecurityUtil.getLoggedUser();

    return findById(analysisHouseId)
        .map(
            existing -> {
              AnalysisHouse analysisHouseToUpdate = AnalysisHouseMapper.toEntity(dto, user);
              analysisHouseToUpdate.setId(analysisHouseId);
              AnalysisHouse updated = save(analysisHouseToUpdate);
              return AnalysisHouseMapper.toDto(updated);
            })
        .orElseThrow(() -> new ResourceNotFoundException("Casa de análise não encontrada"));
  }

  public AnalysisHouseResponseDto getAnalysisHouseById(String id) {
    UUID analysisHouseId = UuidUtil.parseUuid(id);
    return findById(analysisHouseId)
        .map(AnalysisHouseMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Casa de análise não encontrada"));
  }
}
