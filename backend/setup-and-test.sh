#!/bin/bash

echo "🔧 Atualizando dependências e executando testes"
echo "=============================================="

echo ""
echo "📦 1. Baixando dependências do Maven..."
./mvnw dependency:resolve

echo ""
echo "🧹 2. Limpando projeto..."
./mvnw clean

echo ""
echo "⚙️  3. Compilando código principal..."
./mvnw compile

echo ""
echo "🧪 4. Compilando testes..."
./mvnw test-compile

echo ""
echo "🚀 5. Executando teste rápido..."
./mvnw test -Dtest=QuickFinancialValidationTest

echo ""
echo "✅ Processo concluído!"
echo ""
echo "📋 Para executar outros testes:"
echo "   • Teste completo: ./mvnw test -Dtest=SingleLotExitProcessorTest"
echo "   • Teste financeiro: ./mvnw test -Dtest=SingleLotExitProcessorFinancialCalculationTest"
echo "   • Todos os testes: ./mvnw test"
