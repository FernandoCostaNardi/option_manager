package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.Asset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
  Optional<Asset> findByCode(String code);
}
