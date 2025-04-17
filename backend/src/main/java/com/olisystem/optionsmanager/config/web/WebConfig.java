package com.olisystem.optionsmanager.config.web;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

  /** Configura o CORS para permitir requisições do frontend */
  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173", // Vite default
            "http://localhost:4200", // Angular default
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"));
    corsConfiguration.setAllowedHeaders(
        Arrays.asList(
            "Origin",
            "Access-Control-Allow-Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "Origin, Accept",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));
    corsConfiguration.setExposedHeaders(
        Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"));
    corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource =
        new UrlBasedCorsConfigurationSource();
    urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

    return new CorsFilter(urlBasedCorsConfigurationSource);
  }
}
