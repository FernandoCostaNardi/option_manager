package com.olisystem.optionsmanager.service.asset;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.Asset.Asset;

import org.springframework.stereotype.Service;

@Service
public interface  AssetService {
  Asset findOrCreateAsset(OperationDataRequest request);
}
