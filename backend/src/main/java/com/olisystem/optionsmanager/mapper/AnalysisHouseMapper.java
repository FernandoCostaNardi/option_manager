package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.AnalysisHouseCreateRequestDto;
import com.olisystem.optionsmanager.dto.AnalysisHouseResponseDto;
import com.olisystem.optionsmanager.model.AnalysisHouse;
import com.olisystem.optionsmanager.model.StatusType;
import com.olisystem.optionsmanager.model.User;

public class AnalysisHouseMapper {

  public static AnalysisHouse toEntity(AnalysisHouseCreateRequestDto dto, User user) {
    return AnalysisHouse.builder()
        .name(dto.getName())
        .cnpj(dto.getCnpj())
        .website(dto.getWebsite())
        .contactEmail(dto.getContactEmail())
        .contactPhone(dto.getContactPhone())
        .subscriptionType(dto.getSubscriptionType())
        .status(dto.getStatus() != null ? dto.getStatus() : StatusType.ACTIVE)
        .user(user)
        .build();
  }

  public static AnalysisHouseResponseDto toDto(AnalysisHouse entity) {
    return AnalysisHouseResponseDto.builder()
        .id(entity.getId())
        .name(entity.getName())
        .cnpj(entity.getCnpj())
        .website(entity.getWebsite())
        .contactEmail(entity.getContactEmail())
        .contactPhone(entity.getContactPhone())
        .subscriptionType(entity.getSubscriptionType())
        .status(entity.getStatus())
        .build();
  }
}
