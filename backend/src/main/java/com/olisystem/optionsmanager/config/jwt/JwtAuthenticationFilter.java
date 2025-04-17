package com.olisystem.optionsmanager.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;

    log.debug("Cabeçalho de autorização: {}", authHeader);

    // Se não há cabeçalho de autorização ou não começa com "Bearer ", passa para o próximo filtro
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.debug("Cabeçalho de autorização ausente ou inválido");
      filterChain.doFilter(request, response);
      return;
    }

    // Extrai o token JWT do cabeçalho
    jwt = authHeader.substring(7);

    try {
      userEmail = jwtService.extractUsername(jwt);
      log.debug("Email extraído do token: {}", userEmail);

      // Se o email foi extraído e não há autenticação atual
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        // Carrega os detalhes do usuário
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

        // Valida o token
        if (jwtService.isTokenValid(jwt, userDetails)) {
          log.debug("Token válido para usuário: {}", userEmail);

          // Cria um token de autenticação
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          // Define o contexto de segurança
          SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
          log.warn("Token inválido para usuário: {}", userEmail);
        }
      }
    } catch (Exception e) {
      log.error("Erro ao processar token JWT", e);
    }

    filterChain.doFilter(request, response);
  }
}
