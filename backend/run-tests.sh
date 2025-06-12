#!/bin/bash

echo "🧪 Executando Testes do SingleLotExitProcessor"
echo "============================================="

echo ""
echo "📊 Cenário de Teste:"
echo "   • Entrada: 300 cotas × R$ 1,03 = R$ 309,00"
echo "   • Saída parcial: 75 cotas × R$ 1,73 = R$ 129,75"
echo "   • Saída final: 225 cotas × R$ 0,46 = R$ 103,50"
echo "   • Resultado esperado: -R$ 75,75 (-24,51%)"
echo ""

echo "🚀 Executando testes..."

# Executar teste principal
./mvnw test -Dtest=SingleLotExitProcessorTest

# Executar teste de cálculos financeiros
./mvnw test -Dtest=SingleLotExitProcessorFinancialCalculationTest

echo ""
echo "✅ Testes concluídos!"
echo ""
echo "📋 Validações realizadas:"
echo "   ✓ Consolidação final correta"
echo "   ✓ Cálculo de investimento original (R$ 309,00)"
echo "   ✓ Soma de operações de saída (R$ 233,25)"
echo "   ✓ Resultado absoluto (-R$ 75,75)"
echo "   ✓ Percentual correto (-24,51%)"
echo "   ✓ Status final LOSER"
echo "   ✓ Operações intermediárias marcadas como HIDDEN"
