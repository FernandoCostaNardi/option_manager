package com.olisystem.optionsmanager.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BrokerageResponseDto {
  private UUID id;
  private String name;
  private String cnpj;
  private String account;
  private String agency;
  private UserSummaryDto user;
}
