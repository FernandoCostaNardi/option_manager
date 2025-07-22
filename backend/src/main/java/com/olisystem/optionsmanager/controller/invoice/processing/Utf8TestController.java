package com.olisystem.optionsmanager.controller.invoice.processing;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de teste para UTF-8
 * ✅ INTEGRAÇÃO: Sistema de progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-21
 */
@RestController
@RequestMapping("/api/test/utf8")
public class Utf8TestController {

    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Object> testUtf8() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Processando operação 1 de 7...");
        response.put("status", "PROCESSING");
        response.put("current", 1);
        response.put("total", 7);
        response.put("percentage", 14);
        return response;
    }
} 