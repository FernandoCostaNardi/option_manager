package com.olisystem.optionsmanager.service.asset;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.repository.asset.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Override
    @Transactional
    public Asset findOrCreateAsset(OperationDataRequest request) {
        String assetCode = request.getBaseAssetCode();

        // Verifica se o ativo já existe
        return assetRepository.findByCode(assetCode)
                .orElseGet(() -> {
                    // Se não existir, cria um novo ativo usando dados do DTO
                    Asset newAsset = Asset.builder()
                            .code(assetCode)
                            .name(request.getBaseAssetName() != null ?
                                    request.getBaseAssetName() : "Asset " + assetCode)
                            .urlLogo(request.getBaseAssetLogoUrl() != null ?
                                    request.getBaseAssetLogoUrl() : "/default-logo.png")
                            .type(request.getBaseAssetType() != null ?
                                    request.getBaseAssetType() : AssetType.STOCK)
                            .build();

                    // Salva o novo ativo
                    return assetRepository.save(newAsset);
                });
    }
}
