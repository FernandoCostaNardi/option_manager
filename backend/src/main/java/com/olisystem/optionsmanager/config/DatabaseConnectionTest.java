package com.olisystem.optionsmanager.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    try {
      String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
      System.out.println("Conexão com o banco de dados estabelecida com sucesso!");
      System.out.println("Resultado do teste: " + result);
    } catch (Exception e) {
      System.err.println("Erro ao conectar ao banco de dados:");
      System.err.println("Mensagem: " + e.getMessage());
      System.err.println("Causa: " + e.getCause());
    }
  }
}
