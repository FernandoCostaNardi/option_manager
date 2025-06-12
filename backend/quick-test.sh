#!/bin/bash

echo "🧮 Teste Rápido - Validação dos Cálculos Financeiros"
echo "=================================================="

echo ""
echo "📊 Executando validação rápida..."

./mvnw test -Dtest=QuickFinancialValidationTest

echo ""
echo "✅ Validação rápida concluída!"
