package com.olisystem.optionsmanager.mapper.asset;

import com.olisystem.optionsmanager.dto.asset.AssetCreateRequestDto;
import com.olisystem.optionsmanager.dto.asset.AssetResponseDto;
import com.olisystem.optionsmanager.model.Asset.Asset;

public class AssetMapper {

  public static Asset toEntity(AssetCreateRequestDto dto) {
    return Asset.builder().code(dto.getCode()).name(dto.getName()).type(dto.getType()).build();
  }

  public static AssetResponseDto toDto(Asset entity) {
    return AssetResponseDto.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .type(entity.getType())
        .build();
  }
}
