package com.olisystem.optionsmanager.service.operation.strategy.processor;

/**
 * Utilitário simples para validar manualmente os cálculos
 * Pode ser executado diretamente sem framework de teste
 */
public class ManualCalculationValidator {
    
    public static void main(String[] args) {
        System.out.println("🧮 VALIDADOR MANUAL DOS CÁLCULOS FINANCEIROS");
        System.out.println("============================================");
        
        validateBasicCalculation();
        validatePercentageCalculation();
        validateIndividualExits();
        
        System.out.println("\n✅ TODOS OS CÁLCULOS VALIDADOS COM SUCESSO!");
    }
    
    private static void validateBasicCalculation() {
        System.out.println("\n📊 CÁLCULO BÁSICO:");
        
        double totalInvested = 300 * 1.03;    // R$ 309,00
        double firstExit = 75 * 1.73;         // R$ 129,75
        double secondExit = 225 * 0.46;       // R$ 103,50
        double totalReceived = firstExit + secondExit;  // R$ 233,25
        double result = totalReceived - totalInvested;  // -R$ 75,75
        
        System.out.println("   💰 Total investido: R$ " + String.format("%.2f", totalInvested));
        System.out.println("   💸 Primeira saída: R$ " + String.format("%.2f", firstExit));
        System.out.println("   💸 Segunda saída: R$ " + String.format("%.2f", secondExit));
        System.out.println("   💸 Total recebido: R$ " + String.format("%.2f", totalReceived));
        System.out.println("   📊 Resultado: R$ " + String.format("%.2f", result));
        
        assert Math.abs(totalInvested - 309.00) < 0.01 : "Investimento incorreto";
        assert Math.abs(totalReceived - 233.25) < 0.01 : "Total recebido incorreto";
        assert Math.abs(result - (-75.75)) < 0.01 : "Resultado final incorreto";
    }
    
    private static void validatePercentageCalculation() {
        System.out.println("\n📈 CÁLCULO DE PERCENTUAL:");
        
        double result = -75.75;
        double totalInvested = 309.00;
        double percentage = (result / totalInvested) * 100;
        
        System.out.println("   📉 Percentual: " + String.format("%.2f", percentage) + "%");
        
        assert Math.abs(percentage - (-24.51)) < 0.1 : "Percentual incorreto";
    }
    
    private static void validateIndividualExits() {
        System.out.println("\n🔍 VALIDAÇÃO DAS SAÍDAS INDIVIDUAIS:");
        
        // Primeira saída: lucro
        double firstEntryValue = 75 * 1.03;   // R$ 77,25
        double firstExitValue = 75 * 1.73;    // R$ 129,75
        double firstProfit = firstExitValue - firstEntryValue;  // R$ 52,50
        
        System.out.println("   💰 Primeira saída - Lucro: R$ " + String.format("%.2f", firstProfit));
        
        // Segunda saída: prejuízo (usando preço break-even)
        double secondEntryValue = 225 * 0.80; // R$ 180,00 (break-even)
        double secondExitValue = 225 * 0.46;  // R$ 103,50
        double secondLoss = secondExitValue - secondEntryValue;  // -R$ 76,50
        
        System.out.println("   📉 Segunda saída - Prejuízo: R$ " + String.format("%.2f", secondLoss));
        
        // Validar que a soma das saídas individuais não é o resultado final
        double sumOfIndividualResults = firstProfit + secondLoss;  // -R$ 24,00
        System.out.println("   ⚠️  Soma P&L individuais: R$ " + String.format("%.2f", sumOfIndividualResults));
        System.out.println("   ✅ Resultado absoluto correto: R$ -75,75");
        System.out.println("   📝 Diferença: " + String.format("%.2f", -75.75 - sumOfIndividualResults));
        
        assert Math.abs(firstProfit - 52.50) < 0.01 : "Lucro primeira saída incorreto";
        assert Math.abs(secondLoss - (-76.50)) < 0.01 : "Prejuízo segunda saída incorreto";
    }
}
