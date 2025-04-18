package com.olisystem.optionsmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHouseCreateRequestDto {
  private String name;
  private String cnpj;
  private String website;
  private String contactEmail;
  private String contactPhone;
  private String subscriptionType;
}
