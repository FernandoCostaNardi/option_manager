package com.olisystem.optionsmanager.service.operation.creation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationBuildData;
import com.olisystem.optionsmanager.record.operation.OperationBuildExitData;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.service.analysis_house.AnalysisHouseService;
import com.olisystem.optionsmanager.service.brokerage.BrokerageService;
import com.olisystem.optionsmanager.service.operation.target.OperationTargetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class OperationCreationServiceImpl implements OperationCreationService {
    private final BrokerageService brokerageService;
    private final AnalysisHouseService analysisHouseService;
    private final OperationRepository operationRepository;
    private final OperationTargetService targetService;

    // Construtor com injeção de dependências
    public OperationCreationServiceImpl(
            BrokerageService brokerageService,
            AnalysisHouseService analysisHouseService,
            OperationRepository operationRepository,
            OperationTargetService targetService) {
        this.brokerageService = brokerageService;
        this.analysisHouseService = analysisHouseService;
        this.operationRepository = operationRepository;
        this.targetService = targetService;
    }

    @Override
    @Transactional
    public Operation createActiveOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser) {
        log.info("🟢 [DEBUG] TransactionType recebido no createActiveOperation: {}", request.getTransactionType());
        
        // ✅ CORREÇÃO: Validar TransactionType antes de processar
        validateTransactionType(request);
        
        Operation operation = buildOperation(
                OperationBuildData.fromRequest(request),
                optionSerie,
                OperationStatus.ACTIVE,
                currentUser
        );
        
        // ✅ CORREÇÃO: Verificar se TransactionType foi preservado
        if (!operation.getTransactionType().equals(request.getTransactionType())) {
            log.error("❌ ERRO CRÍTICO: TransactionType foi alterado durante construção! Original: {}, Final: {}", 
                request.getTransactionType(), operation.getTransactionType());
            operation.setTransactionType(request.getTransactionType()); // Forçar correção
        }
        
        log.info("🟢 [DEBUG] TransactionType no Operation (antes do save): {}", operation.getTransactionType());
        Operation savedOperation = operationRepository.save(operation);
        log.info("🟢 [DEBUG] TransactionType no Operation (depois do save): {}", savedOperation.getTransactionType());
        
        // ✅ CORREÇÃO: Verificar se TransactionType foi preservado após save
        if (!savedOperation.getTransactionType().equals(request.getTransactionType())) {
            log.error("❌ ERRO CRÍTICO: TransactionType foi alterado após save! Original: {}, Final: {}", 
                request.getTransactionType(), savedOperation.getTransactionType());
            savedOperation.setTransactionType(request.getTransactionType());
            savedOperation = operationRepository.save(savedOperation); // Salvar correção
        }
        
        logOperationCreation(OperationStatus.ACTIVE, savedOperation);
        targetService.processOperationTargets(request, savedOperation);
        return savedOperation;
    }

    @Override
    @Transactional
    public Operation createHiddenOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser) {
        // ✅ CORREÇÃO: Validar TransactionType antes de processar
        validateTransactionType(request);
        
        Operation operation = buildOperation(
                OperationBuildData.fromRequest(request),
                optionSerie,
                OperationStatus.HIDDEN,
                currentUser
        );

        // ✅ CORREÇÃO: Verificar se TransactionType foi preservado
        if (!operation.getTransactionType().equals(request.getTransactionType())) {
            log.error("❌ ERRO CRÍTICO: TransactionType foi alterado durante construção! Original: {}, Final: {}", 
                request.getTransactionType(), operation.getTransactionType());
            operation.setTransactionType(request.getTransactionType()); // Forçar correção
        }

        Operation savedOperation = operationRepository.save(operation);
        
        // ✅ CORREÇÃO: Verificar se TransactionType foi preservado após save
        if (!savedOperation.getTransactionType().equals(request.getTransactionType())) {
            log.error("❌ ERRO CRÍTICO: TransactionType foi alterado após save! Original: {}, Final: {}", 
                request.getTransactionType(), savedOperation.getTransactionType());
            savedOperation.setTransactionType(request.getTransactionType());
            savedOperation = operationRepository.save(savedOperation); // Salvar correção
        }
        
        logOperationCreation(OperationStatus.HIDDEN, savedOperation);

        targetService.processOperationTargets(request, savedOperation);

        return savedOperation;
    }

    @Override
    @Transactional
    public Operation createConsolidatedOperation(Operation originalOperation, OptionSerie optionSerie, User currentUser) {
        Operation operation = buildOperation(
                OperationBuildData.fromOperation(originalOperation),
                optionSerie,
                OperationStatus.ACTIVE,
                currentUser
        );

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(OperationStatus.ACTIVE, savedOperation);

        return savedOperation;
    }

    @Override
    public Operation createExitOperation(OperationExitPositionContext context, TradeType tradeType, BigDecimal profitLoss, TransactionType type, Integer totalQuantity) {
        
        // ✅ CORREÇÃO: Validar dados antes de criar operação
        validateExitOperationData(context, tradeType, profitLoss, type, totalQuantity);
        
        Operation operation = buildExitOperation(
                OperationBuildExitData.fromRequest(context, profitLoss, tradeType, type, totalQuantity),
                context.context().activeOperation().getOptionSeries(),
                profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? OperationStatus.WINNER : OperationStatus.LOSER,
                context.context().currentUser()
        );

        // ✅ CORREÇÃO: Validar operação construída antes de salvar
        validateBuiltExitOperation(operation);

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(savedOperation.getStatus(), savedOperation);
        
        log.debug("Operação de saída criada com sucesso: ID={}, quantidade={}, profitLoss={}, status={}",
                savedOperation.getId(), savedOperation.getQuantity(), savedOperation.getProfitLoss(), savedOperation.getStatus());
        
        return savedOperation;
    }

    private Operation buildExitOperation(OperationBuildExitData data, OptionSerie optionSeries, OperationStatus status, User currentUser) {
        var brokerage = brokerageService.getBrokerageById(data.brokerageId(), currentUser);
        var analysisHouse = data.analysisHouseId() != null ?
                analysisHouseService.findById(data.analysisHouseId(), currentUser).orElse(null) :
                null;

        // 2. Calcular valor total
        BigDecimal entryTotalValue = data.entryUnitPrice()
                .multiply(BigDecimal.valueOf(data.quantity()));

        // 3. Criar a entidade usando Builder
        return Operation.builder()
                .optionSeries(optionSeries)
                .brokerage(brokerage)
                .analysisHouse(analysisHouse)
                .transactionType(data.transactionType())
                .tradeType(data.tradeType())
                .entryDate(data.entryDate())
                .exitDate(data.exitDate())
                .quantity(data.quantity())
                .entryUnitPrice(data.entryUnitPrice())
                .entryTotalValue(entryTotalValue)
                .exitUnitPrice(data.exitUnitPrice())
                .exitTotalValue(data.exitTotalValue())
                .profitLoss(data.profitLoss())
                .profitLossPercentage(data.profitLossPercentage())
                .status(status)
                .user(currentUser)
                .build();
    }
    

    /**
     * Método unificado para construir uma operação, independentemente da fonte de dados
     * ✅ CORREÇÃO: Garantir que TransactionType seja preservado
     */
    private Operation buildOperation(OperationBuildData data, OptionSerie optionSerie,
                                     OperationStatus status, User currentUser) {
        log.debug("🔧 Construindo operação com TransactionType: {}", data.transactionType());
        // Buscar entidades relacionadas usando apenas o ID da corretora
        var brokerage = brokerageService.getBrokerageById(data.brokerageId());
        var analysisHouse = data.analysisHouseId() != null ?
                analysisHouseService.findById(data.analysisHouseId(), currentUser).orElse(null) :
                null;
        BigDecimal entryTotalValue = data.entryUnitPrice()
                .multiply(BigDecimal.valueOf(data.quantity()));
        Operation operation = Operation.builder()
                .optionSeries(optionSerie)
                .brokerage(brokerage)
                .analysisHouse(analysisHouse)
                .transactionType(data.transactionType())
                .entryDate(data.entryDate())
                .quantity(data.quantity())
                .entryUnitPrice(data.entryUnitPrice())
                .entryTotalValue(entryTotalValue)
                .status(status)
                .user(currentUser)
                .build();
        if (!operation.getTransactionType().equals(data.transactionType())) {
            log.error("❌ ERRO: TransactionType não foi definido corretamente! Esperado: {}, Atual: {}", 
                data.transactionType(), operation.getTransactionType());
            operation.setTransactionType(data.transactionType());
        }
        log.debug("✅ Operação construída com TransactionType: {}", operation.getTransactionType());
        return operation;
    }

    @Override
    @Transactional
    public Operation createExitOperationWithSpecificData(
            Operation originalOperation,
            OptionSerie optionSerie,
            User currentUser,
            int quantity,
            BigDecimal entryUnitPrice,
            BigDecimal exitUnitPrice,
            BigDecimal profitLoss,
            BigDecimal profitLossPercentage,
            TradeType tradeType,
            LocalDate exitDate) {
        
        log.debug("Criando operação de saída com dados específicos: quantidade={}, P&L={}, tipo={}", 
                quantity, profitLoss, tradeType);

        // Calcular valores totais
        BigDecimal entryTotalValue = entryUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal exitTotalValue = exitUnitPrice.multiply(BigDecimal.valueOf(quantity));

        // Determinar status baseado no P&L
        OperationStatus status = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? 
            OperationStatus.WINNER : OperationStatus.LOSER;

        // Criar a operação de saída
        Operation exitOperation = Operation.builder()
                .optionSeries(optionSerie)
                .brokerage(originalOperation.getBrokerage())
                .analysisHouse(originalOperation.getAnalysisHouse())
                .transactionType(TransactionType.SELL)
                .tradeType(tradeType)
                .entryDate(originalOperation.getEntryDate())
                .exitDate(exitDate)
                .quantity(quantity)
                .entryUnitPrice(entryUnitPrice)
                .entryTotalValue(entryTotalValue)
                .exitUnitPrice(exitUnitPrice)
                .exitTotalValue(exitTotalValue)
                .profitLoss(profitLoss)
                .profitLossPercentage(profitLossPercentage)
                .status(status)
                .user(currentUser)
                .build();

        Operation savedOperation = operationRepository.save(exitOperation);
        
        log.info("Operação de saída específica criada: id={}, P&L={}, tipo={}", 
                savedOperation.getId(), profitLoss, tradeType);
        
        return savedOperation;
    }

    /**
     * ✅ NOVO MÉTODO: Validar TransactionType antes de processar
     */
    private void validateTransactionType(OperationDataRequest request) {
        if (request.getTransactionType() == null) {
            throw new IllegalArgumentException("TransactionType não pode ser nulo");
        }
        
        log.info("✅ TransactionType validado: {}", request.getTransactionType());
    }

    /**
     * ✅ NOVO MÉTODO: Validar dados antes de criar operação de saída
     */
    private void validateExitOperationData(OperationExitPositionContext context, TradeType tradeType, 
                                          BigDecimal profitLoss, TransactionType type, Integer totalQuantity) {
        
        if (context == null) {
            throw new IllegalArgumentException("Contexto de saída não pode ser nulo");
        }
        
        if (tradeType == null) {
            throw new IllegalArgumentException("Tipo de trade não pode ser nulo");
        }
        
        if (profitLoss == null) {
            throw new IllegalArgumentException("Valor de lucro/prejuízo não pode ser nulo");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Tipo de transação não pode ser nulo");
        }
        
        if (totalQuantity == null || totalQuantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero. Valor recebido: " + totalQuantity);
        }
        
        if (context.context() == null || context.context().request() == null) {
            throw new IllegalArgumentException("Request de finalização não pode ser nulo");
        }
        
        log.debug("Validação de dados aprovada para criação de operação de saída");
    }

    /**
     * ✅ NOVO MÉTODO: Validar operação construída antes de salvar
     */
    private void validateBuiltExitOperation(Operation operation) {
        
        if (operation == null) {
            throw new IllegalArgumentException("Operação construída não pode ser nula");
        }
        
        // ✅ CORREÇÃO: Rejeitar operações com quantidade zero
        if (operation.getQuantity() == null || operation.getQuantity() <= 0) {
            throw new IllegalArgumentException("Operação com quantidade inválida será rejeitada. Quantidade: " + operation.getQuantity());
        }
        
        // ✅ CORREÇÃO: Rejeitar operações com valores de entrada zerados indevidamente
        if (operation.getEntryTotalValue() == null || operation.getEntryTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("ATENÇÃO: Operação com valor de entrada zerado ou negativo será rejeitada. Valor: {}", operation.getEntryTotalValue());
            throw new IllegalArgumentException("Operação com valor de entrada inválido será rejeitada");
        }
        
        // ✅ CORREÇÃO: Rejeitar operações com valores de saída zerados indevidamente
        if (operation.getExitTotalValue() == null || operation.getExitTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("ATENÇÃO: Operação com valor de saída zerado ou negativo será rejeitada. Valor: {}", operation.getExitTotalValue());
            throw new IllegalArgumentException("Operação com valor de saída inválido será rejeitada");
        }
        
        // Validar consistência de datas
        if (operation.getEntryDate() == null || operation.getExitDate() == null) {
            throw new IllegalArgumentException("Datas de entrada e saída são obrigatórias");
        }
        
        if (operation.getExitDate().isBefore(operation.getEntryDate())) {
            throw new IllegalArgumentException("Data de saída não pode ser anterior à data de entrada");
        }
        
        log.debug("Validação da operação construída aprovada: quantidade={}, valor_entrada={}, valor_saida={}",
                operation.getQuantity(), operation.getEntryTotalValue(), operation.getExitTotalValue());
    }

    private void logOperationCreation(OperationStatus status, Operation operation) {
        log.info("Nova operação {} criada: id={}, quantidade={}, preço={}",
                status, operation.getId(), operation.getQuantity(), operation.getEntryUnitPrice());
    }

}

