package com.olisystem.optionsmanager.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingService {

  public void logRequest(HttpServletRequest request) {
    log.info("Requisição recebida: {} {}", request.getMethod(), request.getRequestURI());
    log.info("Cabeçalhos de autorização: {}", request.getHeader("Authorization"));
  }

  public void logAccessDenied(String username, String requestURI) {
    log.warn("Acesso negado para usuário: {} na URI: {}", username, requestURI);
  }
}
