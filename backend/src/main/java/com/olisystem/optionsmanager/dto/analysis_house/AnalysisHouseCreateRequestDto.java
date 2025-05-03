package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.StatusType;
import lombok.Data;

@Data
public class AnalysisHouseCreateRequestDto {
  private String name;
  private String cnpj;
  private String website;
  private String contactEmail;
  private String contactPhone;
  private String subscriptionType;
  private StatusType status;
}
