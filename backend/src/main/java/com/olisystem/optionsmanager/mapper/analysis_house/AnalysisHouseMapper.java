package com.olisystem.optionsmanager.mapper.analysis_house;

import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseCreateRequestDto;
import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseResponseDto;
import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.StatusType;

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
