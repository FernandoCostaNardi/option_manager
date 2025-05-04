package com.olisystem.optionsmanager.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDto {
  private UUID id;
  private String username;
}
