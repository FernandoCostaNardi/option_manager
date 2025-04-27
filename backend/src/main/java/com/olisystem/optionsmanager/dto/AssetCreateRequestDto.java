package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.AssetType;
import lombok.Data;

@Data
public class AssetCreateRequestDto {
  private String code;
  private String name;
  private AssetType type;
  private String sector;
}
