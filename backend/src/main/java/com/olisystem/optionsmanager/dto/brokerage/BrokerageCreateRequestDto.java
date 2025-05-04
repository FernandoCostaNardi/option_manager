package com.olisystem.optionsmanager.dto.brokerage;

import lombok.Data;

@Data
public class BrokerageCreateRequestDto {
  private String name;
  private String cnpj;
  private String account;
  private String agency;
}
