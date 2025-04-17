package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.repository.BrokerageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrokerageService {

  private final BrokerageRepository brokerageRepository;

  public Optional<Brokerage> findByCnpj(String cnpj) {
    return brokerageRepository.findByCnpj(cnpj);
  }

  public Brokerage save(Brokerage brokerage) {
    return brokerageRepository.save(brokerage);
  }

  public void deleteById(UUID id) {
    brokerageRepository.deleteById(id);
  }

  public Optional<Brokerage> findById(UUID id) {
    return brokerageRepository.findById(id);
  }

  public List<Brokerage> findAll() {
    return brokerageRepository.findAll();
  }

  // Add these methods to your BrokerageService class

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
}
