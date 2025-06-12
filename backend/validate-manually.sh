#!/bin/bash

echo "🧮 Validador Manual dos Cálculos"
echo "==============================="

cd "src/test/java/com/olisystem/optionsmanager/service/operation/strategy/processor"

javac ManualCalculationValidator.java
java ManualCalculationValidator

echo ""
echo "✅ Validação manual concluída!"
