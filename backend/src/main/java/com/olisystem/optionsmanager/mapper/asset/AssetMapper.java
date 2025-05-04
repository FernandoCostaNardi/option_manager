package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.AssetCreateRequestDto;
import com.olisystem.optionsmanager.dto.AssetResponseDto;
import com.olisystem.optionsmanager.model.Asset;

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
