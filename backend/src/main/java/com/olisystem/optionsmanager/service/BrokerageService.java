package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.repository.BrokerageRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BrokerageService {

  @Autowired private BrokerageRepository brokerageRepository;

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
}
