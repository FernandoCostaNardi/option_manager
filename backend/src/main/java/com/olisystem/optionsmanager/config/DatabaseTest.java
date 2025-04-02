package com.olisystem.optionsmanager.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://sysconard.com.br/sysconar_marketmanagent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true&characterEncoding=UTF-8&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false";
        String username = "sysconar_marketmanagent";
        String password = "#1F1e0r8n0.";

        try {
            System.out.println("Tentando conectar ao banco de dados...");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("Conexão estabelecida com sucesso!");
            conn.close();
        } catch (Exception e) {
            System.err.println("Erro ao conectar ao banco de dados:");
            System.err.println("Mensagem: " + e.getMessage());
            System.err.println("Causa: " + e.getCause());
        }
    }
} 