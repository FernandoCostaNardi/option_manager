package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.Asset;
import com.olisystem.optionsmanager.repository.AssetRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {

  private final AssetRepository assetRepository;

  public List<Asset> findAll() {
    return assetRepository.findAll();
  }

  public Optional<Asset> findById(UUID id) {
    return assetRepository.findById(id);
  }

  public Optional<Asset> findByCode(String code) {
    return assetRepository.findByCode(code);
  }

  public Asset save(Asset asset) {
    return assetRepository.save(asset);
  }

  public void deleteById(UUID id) {
    assetRepository.deleteById(id);
  }
}
