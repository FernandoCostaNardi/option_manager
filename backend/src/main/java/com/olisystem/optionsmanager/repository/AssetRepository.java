package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.Asset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

  Asset findByCode(String code);
}
