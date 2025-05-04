package com.olisystem.optionsmanager.service.asset;

import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssetService {

  @Autowired private AssetRepository assetRepository;

  public Asset getAssetByCode(String code) {
    return assetRepository.findByCode(code);
  }

  public Asset save(Asset asset) {
    return assetRepository.save(asset);
  }
}
