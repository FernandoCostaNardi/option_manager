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
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AnalysisHouseService {

  @Autowired private AnalysisHouseRepository analysisHouseRepository;

  public List<AnalysisHouse> findAll() {
    User user = SecurityUtil.getLoggedUser();
    return analysisHouseRepository.findByUser(user);
  }

  public Page<AnalysisHouse> findAll(Pageable pageable, String name) {
    User user = SecurityUtil.getLoggedUser();
    // Filtra pelo usuário e demais critérios
    if (name != null && !name.isEmpty()) {
      return analysisHouseRepository.findByNameContainingIgnoreCaseAndUser(name, user, pageable);
    } else if (name != null && !name.isEmpty()) {
      return analysisHouseRepository.findByNameContainingIgnoreCaseAndUser(name, user, pageable);
    } else {
      return analysisHouseRepository.findByUser(user, pageable);
    }
  }

  public Optional<AnalysisHouse> findById(UUID id) {
    User user = SecurityUtil.getLoggedUser();
    return findById(id, user);
  }

  public Optional<AnalysisHouse> findById(UUID id, User user) {
    return analysisHouseRepository.findByIdAndUser(id, user);
  }

  public Optional<AnalysisHouse> findByName(String name) {
    User user = SecurityUtil.getLoggedUser();
    return analysisHouseRepository.findByNameAndUser(name, user);
  }

  public AnalysisHouse save(AnalysisHouse analysisHouse) {
    User user = SecurityUtil.getLoggedUser();
    analysisHouse.setUser(user);
    // validar nome
    if (analysisHouseRepository.findByNameAndUser(analysisHouse.getName(), user).isPresent()) {
      throw new RuntimeException("Nome já cadastrado");
    }
    // validar nome (usando busca paginada sem paginação)
    Page<AnalysisHouse> nameMatches =
        analysisHouseRepository.findByNameContainingIgnoreCaseAndUser(
            analysisHouse.getName(), user, Pageable.unpaged());
    if (nameMatches.hasContent()) {
      throw new RuntimeException("Nome já cadastrado");
    }
    return analysisHouseRepository.save(analysisHouse);
  }

  public void deleteById(UUID id) {
    User user = SecurityUtil.getLoggedUser();
    AnalysisHouse existing =
        analysisHouseRepository
            .findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Casa de análise não encontrada"));
    analysisHouseRepository.delete(existing);
  }

  public void deleteById(String id) {
    UUID analysisHouseId = UuidUtil.parseUuid(id);
    deleteById(analysisHouseId);
  }

  public AnalysisHouseResponseDto createAnalysisHouse(AnalysisHouseCreateRequestDto dto) {
    User user = SecurityUtil.getLoggedUser();
    AnalysisHouse analysisHouse = AnalysisHouseMapper.toEntity(dto, user);

    return findByName(analysisHouse.getName())
        .map(
            existing -> {
              log.warn(
                  "Tentativa de criar casa de análise com Nome duplicado: {}",
                  analysisHouse.getName());
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
