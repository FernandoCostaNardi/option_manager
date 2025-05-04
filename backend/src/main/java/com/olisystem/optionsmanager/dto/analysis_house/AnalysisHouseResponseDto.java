package com.olisystem.optionsmanager.dto.analysis_house;

import com.olisystem.optionsmanager.model.StatusType;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisHouseResponseDto {
  private UUID id;
  private String name;
  private String cnpj;
  private String website;
  private String contactEmail;
  private String contactPhone;
  private String subscriptionType;
  private StatusType status;
}
