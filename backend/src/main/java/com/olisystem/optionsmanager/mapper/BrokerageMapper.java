package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.BrokerageCreateRequestDto;
import com.olisystem.optionsmanager.dto.BrokerageResponseDto;
import com.olisystem.optionsmanager.dto.UserSummaryDto;
import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.model.User;

public class BrokerageMapper {

  public static Brokerage toEntity(BrokerageCreateRequestDto dto, User user) {
    return Brokerage.builder()
        .name(dto.getName())
        .cnpj(dto.getCnpj())
        .account(dto.getAccount())
        .agency(dto.getAgency())
        .user(user)
        .build();
  }

  public static BrokerageResponseDto toDto(Brokerage brokerage) {
    User user = brokerage.getUser();
    UserSummaryDto userSummary = null;
    if (user != null) {
      userSummary = new UserSummaryDto(user.getId(), user.getUsername());
    }
    return new BrokerageResponseDto(
        brokerage.getId(),
        brokerage.getName(),
        brokerage.getCnpj(),
        brokerage.getAccount(),
        brokerage.getAgency(),
        userSummary);
  }
}
