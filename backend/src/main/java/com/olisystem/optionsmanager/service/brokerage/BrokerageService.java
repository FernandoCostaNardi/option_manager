package com.olisystem.optionsmanager.service.brokerage;

import com.olisystem.optionsmanager.dto.brokerage.BrokerageCreateRequestDto;
import com.olisystem.optionsmanager.dto.brokerage.BrokerageResponseDto;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.mapper.brokerage.BrokerageMapper;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.repository.BrokerageRepository;
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
public class BrokerageService {

  @Autowired private BrokerageRepository brokerageRepository;

  public List<Brokerage> findAll() {
    return brokerageRepository.findAll();
  }

  public Page<Brokerage> findAll(Pageable pageable, String name, String cnpj) {
    if (name != null && !name.isEmpty() && cnpj != null && !cnpj.isEmpty()) {
      return brokerageRepository.findByNameContainingIgnoreCaseAndCnpjContainingIgnoreCase(
          name, cnpj, pageable);
    } else if (name != null && !name.isEmpty()) {
      return brokerageRepository.findByNameContainingIgnoreCase(name, pageable);
    } else if (cnpj != null && !cnpj.isEmpty()) {
      return brokerageRepository.findByCnpjContainingIgnoreCase(cnpj, pageable);
    } else {
      return brokerageRepository.findAll(pageable);
    }
  }

  public Optional<Brokerage> findById(UUID id) {
    return brokerageRepository.findById(id);
  }

  public Optional<Brokerage> findByCnpj(String cnpj) {
    return brokerageRepository.findByCnpj(cnpj);
  }

  public Brokerage getBrokerageById(UUID id) {
    return brokerageRepository.findById(id).orElse(null);
  }

  public Brokerage save(Brokerage brokerage) {
    return brokerageRepository.save(brokerage);
  }

  public void deleteById(UUID id) {
    brokerageRepository.deleteById(id);
  }

  public void deleteById(String id) {
    UUID brokerageId = UuidUtil.parseUuid(id);
    deleteById(brokerageId);
  }

  public BrokerageResponseDto createBrokerage(BrokerageCreateRequestDto dto) {
    User user = SecurityUtil.getLoggedUser();
    Brokerage brokerage = BrokerageMapper.toEntity(dto, user);

    return findByCnpj(brokerage.getCnpj())
        .map(
            existing -> {
              log.warn("Tentativa de criar corretora com CNPJ duplicado: {}", brokerage.getCnpj());
              return BrokerageMapper.toDto(existing);
            })
        .orElseGet(
            () -> {
              Brokerage savedBrokerage = save(brokerage);
              return BrokerageMapper.toDto(savedBrokerage);
            });
  }

  public BrokerageResponseDto updateBrokerage(String id, BrokerageCreateRequestDto dto) {
    UUID brokerageId = UuidUtil.parseUuid(id);
    User user = SecurityUtil.getLoggedUser();

    return findById(brokerageId)
        .map(
            existing -> {
              Brokerage brokerageToUpdate = BrokerageMapper.toEntity(dto, user);
              brokerageToUpdate.setId(brokerageId);
              Brokerage updated = save(brokerageToUpdate);
              return BrokerageMapper.toDto(updated);
            })
        .orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada"));
  }

  public BrokerageResponseDto getBrokerageById(String id) {
    UUID brokerageId = UuidUtil.parseUuid(id);
    return findById(brokerageId)
        .map(BrokerageMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada"));
  }
}
