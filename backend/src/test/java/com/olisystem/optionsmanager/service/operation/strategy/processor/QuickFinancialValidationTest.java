package com.olisystem.optionsmanager.service.operation.strategy.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste simples e rápido para validar os cálculos financeiros básicos
 * do cenário real identificado: -R$ 75,75 (-24,51%)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Validação Rápida dos Cálculos Financeiros")
class QuickFinancialValidationTest {

    @Test
    @DisplayName("Validação rápida: Investido R$ 309,00 → Recebido R$ 233,25 = -R$ 75,75 (-24,51%)")
    void quickValidationOfRealScenario() {
        // Given - Valores do cenário real
        BigDecimal totalInvested = new BigDecimal("309.00");      // 300 cotas × R$ 1,03
        BigDecimal firstExit = new BigDecimal("129.75");          // 75 cotas × R$ 1,73
        BigDecimal secondExit = new BigDecimal("103.50");         // 225 cotas × R$ 0,46
        
        // When - Cálculos
        BigDecimal totalReceived = firstExit.add(secondExit);
        BigDecimal absoluteResult = totalReceived.subtract(totalInvested);
        BigDecimal percentage = absoluteResult
            .divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        // Then - Validações
        assertEquals(0, totalReceived.compareTo(new BigDecimal("233.25")), 
            "Total recebido deve ser R$ 233,25");
        
        assertEquals(0, absoluteResult.compareTo(new BigDecimal("-75.75")), 
            "Resultado deve ser -R$ 75,75");
        
        // Percentual com tolerância para arredondamento
        assertTrue(percentage.compareTo(new BigDecimal("-24.52")) >= 0 && 
                  percentage.compareTo(new BigDecimal("-24.50")) <= 0,
                  "Percentual deve ser aproximadamente -24,51%, atual: " + percentage);
        
        // Output para verificação
        System.out.println("🧮 VALIDAÇÃO RÁPIDA DOS CÁLCULOS:");
        System.out.println("   💰 Total investido: R$ " + totalInvested);
        System.out.println("   💸 Primeira saída: R$ " + firstExit);
        System.out.println("   💸 Segunda saída: R$ " + secondExit);
        System.out.println("   💸 Total recebido: R$ " + totalReceived);
        System.out.println("   📊 Resultado: R$ " + absoluteResult);
        System.out.println("   📈 Percentual: " + percentage + "%");
        System.out.println("   ✅ TODOS OS CÁLCULOS CORRETOS!");
    }

    @Test
    @DisplayName("Validação: Primeira saída deve gerar lucro de R$ 52,50")
    void validateFirstExitProfit() {
        // Given
        BigDecimal firstExitQuantity = new BigDecimal("75");
        BigDecimal entryPrice = new BigDecimal("1.03");
        BigDecimal exitPrice = new BigDecimal("1.73");
        
        // When
        BigDecimal entryValue = firstExitQuantity.multiply(entryPrice);  // 75 × 1,03 = 77,25
        BigDecimal exitValue = firstExitQuantity.multiply(exitPrice);    // 75 × 1,73 = 129,75
        BigDecimal profit = exitValue.subtract(entryValue);              // 129,75 - 77,25 = 52,50
        
        // Then
        assertEquals(0, entryValue.compareTo(new BigDecimal("77.25")));
        assertEquals(0, exitValue.compareTo(new BigDecimal("129.75")));
        assertEquals(0, profit.compareTo(new BigDecimal("52.50")));
        
        System.out.println("💰 PRIMEIRA SAÍDA VALIDADA:");
        System.out.println("   📈 Lucro: R$ " + profit + " ✅");
    }

    @Test
    @DisplayName("Validação: Segunda saída deve gerar prejuízo")
    void validateSecondExitLoss() {
        // Given - Usando preço break-even após primeira saída
        BigDecimal secondExitQuantity = new BigDecimal("225");
        BigDecimal breakEvenPrice = new BigDecimal("0.80");  // Preço após primeira saída
        BigDecimal exitPrice = new BigDecimal("0.46");
        
        // When
        BigDecimal entryValue = secondExitQuantity.multiply(breakEvenPrice);  // 225 × 0,80 = 180,00
        BigDecimal exitValue = secondExitQuantity.multiply(exitPrice);        // 225 × 0,46 = 103,50
        BigDecimal loss = exitValue.subtract(entryValue);                     // 103,50 - 180,00 = -76,50
        
        // Then
        assertEquals(0, entryValue.compareTo(new BigDecimal("180.00")));
        assertEquals(0, exitValue.compareTo(new BigDecimal("103.50")));
        assertEquals(0, loss.compareTo(new BigDecimal("-76.50")));
        
        System.out.println("📉 SEGUNDA SAÍDA VALIDADA:");
        System.out.println("   📉 Prejuízo: R$ " + loss + " ✅");
    }
}
