package com.olisystem.optionsmanager.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.olisystem.optionsmanager.model.Asset.Asset;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

  Asset findByCode(String code);
}
