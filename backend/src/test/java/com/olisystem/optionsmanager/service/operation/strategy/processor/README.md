# Testes do SingleLotExitProcessor

## 📊 Cenário de Teste

Este conjunto de testes valida o cenário real onde foram identificados problemas de cálculo:

### Operações:
1. **Entrada inicial**: 300 cotas × R$ 1,03 = **R$ 309,00**
2. **Saída parcial**: 75 cotas × R$ 1,73 = **R$ 129,75** (lucro: R$ 52,50)
3. **Saída final**: 225 cotas × R$ 0,46 = **R$ 103,50** (prejuízo: R$ 76,50)

### Resultado Esperado:
- **Total Investido**: R$ 309,00
- **Total Recebido**: R$ 233,25 (R$ 129,75 + R$ 103,50)
- **Resultado Final**: **-R$ 75,75 (-24,51%)**
- **Status**: LOSER
- **Operações intermediárias**: HIDDEN

## 🧪 Testes Implementados

### 1. SingleLotExitProcessorTest
**Classe principal de teste com mocks completos**

**Testes:**
- `shouldConsolidateFinalOperationWithCorrectAbsoluteResult()` - Valida consolidação
- `shouldCalculateCorrectAbsoluteResult()` - Valida valores financeiros
- `shouldIdentifyOriginalOperationForInvestmentCalculation()` - Valida identificação da operação original

### 2. SingleLotExitProcessorFinancialCalculationTest
**Testes focados especificamente nos cálculos financeiros**

**Testes:**
- `shouldCalculateCorrectAbsoluteResultForRealScenario()` - Valida resultado -R$ 75,75 (-24,51%)
- `shouldIdentifyOriginalOperationInGroup()` - Valida identificação da operação ORIGINAL
- `shouldSumExitOperationsCorrectly()` - Valida soma das operações de saída

## 🚀 Como Executar

### Opção 1: Script automático
```bash
chmod +x run-tests.sh
./run-tests.sh
```

### Opção 2: Maven diretamente
```bash
# Teste principal
./mvnw test -Dtest=SingleLotExitProcessorTest

# Teste de cálculos financeiros  
./mvnw test -Dtest=SingleLotExitProcessorFinancialCalculationTest

# Todos os testes do processador
./mvnw test -Dtest="*SingleLotExitProcessor*"
```

### Opção 3: IDE
- Abrir as classes de teste na IDE
- Executar individualmente ou em conjunto
- Observar os logs de validação

## 📋 Validações Realizadas

### ✅ Cálculos Financeiros:
- [x] Investimento original: R$ 309,00 (só operação ORIGINAL)
- [x] Total recebido: R$ 233,25 (soma das saídas)
- [x] Resultado absoluto: -R$ 75,75
- [x] Percentual: -24,51%

### ✅ Consolidação:
- [x] Operação consolidada final criada
- [x] Status LOSER (prejuízo)
- [x] Operações intermediárias marcadas como HIDDEN
- [x] AverageOperationGroup atualizado

### ✅ Identificação de Operações:
- [x] Operação ORIGINAL encontrada corretamente
- [x] Operações SELL somadas sem duplicatas
- [x] Filtros de roleType aplicados

## 🔧 Correções Implementadas

Os testes validam as correções feitas no `SingleLotExitProcessor`:

1. **calculateAbsoluteFinalResult()** - Versão simplificada que:
   - Usa apenas operação ORIGINAL para investimento
   - Soma operações de saída sem duplicatas
   - Inclui saída atual no cálculo

2. **calculateAbsoluteFinalPercentage()** - Usa operação ORIGINAL como base

3. **createFinalConsolidatedOperation()** - Aplica valores absolutos corretos

## 📈 Resultado Esperado dos Testes

```
✅ Cálculos financeiros validados:
   💰 Investimento original: R$ 309.00
   💸 Total recebido: R$ 233.25
   📊 Resultado final: R$ -75.75
   📈 Percentual: -24.51%

✅ Operação ORIGINAL identificada: R$ 309.00
✅ Total recebido calculado: R$ 233.25
✅ Teste de consolidação final passou com sucesso!
```

## 🎯 Benefícios dos Testes

1. **Validação automática** dos cálculos financeiros corretos
2. **Regressão** - Evita que bugs retornem
3. **Documentação viva** do comportamento esperado
4. **Confiança** nas correções implementadas
5. **Facilita manutenção** futura do código
