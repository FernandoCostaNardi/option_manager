package com.olisystem.optionsmanager.repository.asset;

import com.olisystem.optionsmanager.model.Asset.Asset;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

  Optional<Asset> findByCode(String code);
  boolean existsByCode(String code);
}
