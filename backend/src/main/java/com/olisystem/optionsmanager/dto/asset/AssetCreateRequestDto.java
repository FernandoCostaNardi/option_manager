package com.olisystem.optionsmanager.dto.asset;

import com.olisystem.optionsmanager.model.Asset.AssetType;
import lombok.Data;

@Data
public class AssetCreateRequestDto {
  private String code;
  private String name;
  private AssetType type;
  private String sector;
}
