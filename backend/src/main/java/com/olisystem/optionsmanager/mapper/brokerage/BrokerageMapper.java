package com.olisystem.optionsmanager.mapper.brokerage;

import com.olisystem.optionsmanager.dto.auth.UserSummaryDto;
import com.olisystem.optionsmanager.dto.brokerage.BrokerageCreateRequestDto;
import com.olisystem.optionsmanager.dto.brokerage.BrokerageResponseDto;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;

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
