package com.olisystem.optionsmanager.dto.asset;

import java.util.UUID;

import com.olisystem.optionsmanager.model.Asset.AssetType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetResponseDto {
  private UUID id;
  private String code;
  private String name;
  private AssetType type;
  private String sector;
}
