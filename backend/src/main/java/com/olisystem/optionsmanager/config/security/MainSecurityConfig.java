package com.olisystem.optionsmanager.config.security;


import java.util.Arrays;
import java.util.List;

import com.olisystem.optionsmanager.config.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// seu projeto

@Configuration
@EnableWebSecurity
// O nome da classe deve corresponder ao nome do arquivo
public class MainSecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public MainSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean(name = "mainSecurityFilterChain")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Desabilita CSRF porque estamos usando tokens JWT
    http.csrf(csrf -> csrf.disable());

    // Usa configuração CORS global do CorsConfig.java
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

    // Define política de sessão como STATELESS
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    // Configura regras de autorização
    http.authorizeHttpRequests(
        auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/invoice-processing/dashboard/**").permitAll() // Dashboard sem auth
            .requestMatchers("/api/ocr/**").permitAll() // OCR sem auth (quando implementado)
            .requestMatchers("/api/processing/**").permitAll() // Todo processing sem auth para teste
            .anyRequest().authenticated());
    // Adicione o filtro JWT antes do UsernamePasswordAuthenticationFilter
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*")); // Permite qualquer porta localhost
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("*")); // Para SSE funcionar
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L); // Cache preflight por 1 hora
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
