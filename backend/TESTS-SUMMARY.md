# 🧪 Testes Implementados para SingleLotExitProcessor

## 📂 Arquivos Criados:

### 1. **SingleLotExitProcessorTest.java**
- **Tipo**: Teste completo com mocks
- **Foco**: Validação do fluxo completo de consolidação
- **Execução**: `./mvnw test -Dtest=SingleLotExitProcessorTest`

### 2. **SingleLotExitProcessorFinancialCalculationTest.java**
- **Tipo**: Teste de cálculos financeiros
- **Foco**: Validação dos valores específicos (-R$ 75,75, -24,51%)
- **Execução**: `./mvnw test -Dtest=SingleLotExitProcessorFinancialCalculationTest`

### 3. **QuickFinancialValidationTest.java**
- **Tipo**: Teste rápido e simples
- **Foco**: Validação básica dos cálculos sem dependências
- **Execução**: `./mvnw test -Dtest=QuickFinancialValidationTest`

## 🚀 Scripts de Execução:

### 1. **run-tests.sh**
- Executa todos os testes com relatório completo

### 2. **quick-test.sh**
- Executa apenas o teste rápido

## 📊 Cenário Validado:

- **Entrada**: 300 cotas × R$ 1,03 = **R$ 309,00**
- **Saída 1**: 75 cotas × R$ 1,73 = **R$ 129,75**
- **Saída 2**: 225 cotas × R$ 0,46 = **R$ 103,50**
- **Resultado**: **-R$ 75,75 (-24,51%)**

## ✅ Execução Rápida:

```bash
chmod +x quick-test.sh
./quick-test.sh
```
