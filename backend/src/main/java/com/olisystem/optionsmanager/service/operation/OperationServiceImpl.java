package com.olisystem.optionsmanager.service.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.record.operation.OperationContext;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationTargetRepository;
import com.olisystem.optionsmanager.resolver.operation.ExitOperationStrategyResolver;
import com.olisystem.optionsmanager.service.analysis_house.AnalysisHouseService;
import com.olisystem.optionsmanager.service.asset.AssetService;
import com.olisystem.optionsmanager.service.brokerage.BrokerageService;
import com.olisystem.optionsmanager.service.operation.filter.OperationFilterService;
import com.olisystem.optionsmanager.service.operation.strategy.ExitOperationStrategy;
import com.olisystem.optionsmanager.service.operation.strategy.OperationStrategyService;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import com.olisystem.optionsmanager.util.SecurityUtil;
import com.olisystem.optionsmanager.validation.OperationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperationServiceImpl implements OperationService {
    private final OperationValidator operationValidator;
    private final AssetService assetService;
    private final OptionSerieService optionSerieService;
    private final OperationRepository operationRepository;
    private final OperationStrategyService strategyService;
    private final BrokerageService brokerageService;
    private final AnalysisHouseService analysisHouseService;
    private final OperationTargetRepository operationTargetRepository;
    private final OperationFilterService operationFilterService;
    private final ExitOperationStrategyResolver exitOperationStrategyResolver;

    // Construtor com injeção de dependências
    public OperationServiceImpl(
            OperationValidator operationValidator,
            AssetService assetService,
            OptionSerieService optionSerieService,
            OperationRepository operationRepository,
            OperationStrategyService strategyService,
            BrokerageService brokerageService,
            AnalysisHouseService analysisHouseService,
            OperationTargetRepository operationTargetRepository,
            OperationFilterService operationFilterService,
            ExitOperationStrategyResolver exitOperationStrategyResolver
            ) {
        this.operationValidator = operationValidator;
        this.assetService = assetService;
        this.optionSerieService = optionSerieService;
        this.operationRepository = operationRepository;
        this.strategyService = strategyService;
        this.brokerageService = brokerageService;
        this.analysisHouseService = analysisHouseService;
        this.operationTargetRepository = operationTargetRepository;
        this.operationFilterService = operationFilterService;
        this.exitOperationStrategyResolver = exitOperationStrategyResolver;
    }

    @Override
    @Transactional
    public Operation createOperation(OperationDataRequest request) {
        log.info("Iniciando criação de operação com quantidade {} e preço {}",
                request.getQuantity(), request.getEntryUnitPrice());

        // 1. Validar e preparar recursos básicos
        operationValidator.validateCreate(request);
        final User currentUser = SecurityUtil.getLoggedUser();

        // 2. Criar ou recuperar entidades base
        Asset asset = assetService.findOrCreateAsset(request);
        OptionSerie optionSerie = optionSerieService.findOrCreateOptionSerie(request, asset);

        // 3. Criar contexto básico reutilizável
        OperationContext context = new OperationContext(request, optionSerie, currentUser);

        // 4. Determinar estratégia de operação
        Operation activeOperation = operationRepository.findByOptionSeriesAndUserAndStatus(
                optionSerie, currentUser, OperationStatus.ACTIVE);

        // 5. Delegar à estratégia adequada
        if (activeOperation == null) {
            return strategyService.processNewOperation(context);
        } else {
            return strategyService.processExistingOperation(context, activeOperation);
        }
    }

    @Override
    @Transactional
    public Operation createExitOperation(OperationFinalizationRequest request) {
        log.info("Processando finalização de operação: {}", request.getOperationId());

        // 1. Buscar operação ativa
        Operation activeOperation = findActiveOperation(request.getOperationId());

        // 2. Criar contexto de execução
        OperationExitContext context = createExitContext(request, activeOperation);

        // 3. Resolver estratégia apropriada e processar
        ExitOperationStrategy strategy = exitOperationStrategyResolver.resolveStrategy(context);

        return strategy.process(context);
    }

    private Operation findActiveOperation(UUID operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada com ID: " + operationId));
    }

    private OperationExitContext createExitContext(OperationFinalizationRequest request, Operation activeOperation) {
        User currentUser = SecurityUtil.getLoggedUser();
        return new OperationExitContext(request, activeOperation, currentUser);
    }

    @Override
    @Transactional
    public void updateOperation(UUID id, OperationDataRequest request) {
        operationValidator.validateUpdate(request);

        Operation operation =
                operationRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada"));

        // Atualiza os campos básicos
        operation.setTransactionType(request.getTransactionType());
        operation.setEntryDate(request.getEntryDate());
        operation.setExitDate(request.getExitDate());
        operation.setQuantity(request.getQuantity());
        operation.setEntryUnitPrice(request.getEntryUnitPrice());
        operation.setEntryTotalValue(
                request.getEntryUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));

        // Atualiza o tipo de trade se necessário
        if (request.getExitDate() != null) {
            operation.setTradeType(calculateTradeType(request.getEntryDate(), request.getExitDate()));
        }

        // Atualiza a corretora
        if (request.getBrokerageId() != null) {
            Brokerage brokerage = brokerageService.getBrokerageById(request.getBrokerageId());
            operation.setBrokerage(brokerage);
        }

        // Atualiza a casa de análise
        if (request.getAnalysisHouseId() != null) {
            Optional<AnalysisHouse> analysisHouseOpt =
                    analysisHouseService.findById(request.getAnalysisHouseId());
            analysisHouseOpt.ifPresent(operation::setAnalysisHouse);
        }

        // Salva a operação atualizada
        operationRepository.save(operation);

        // Atualiza os targets
        if (request.getTargets() != null) {
            updateOperationTargets(operation, request.getTargets());
        }
    }

    @Override
    public OperationSummaryResponseDto findByFilters(OperationFilterCriteria criteria, Pageable pageable) {
        return operationFilterService.findByFilters(criteria, pageable);
    }

    @Override
    public OperationSummaryResponseDto findByStatuses(List<OperationStatus> status, Pageable pageable) {
        OperationFilterCriteria criteria = OperationFilterCriteria.builder()
                .status(status)
                .build();
        return findByFilters(criteria, pageable);
    }

    private void updateOperationTargets(Operation operation, List<OperationTarget> newTargets) {
        // Remove os targets existentes
        operationTargetRepository.deleteByOperation(operation);

        // Salva os novos targets
        if (!newTargets.isEmpty()) {
            List<OperationTarget> targets =
                    newTargets.stream()
                            .map(
                                    targetDto -> {
                                        OperationTarget target = new OperationTarget();
                                        target.setSequence(targetDto.getSequence());
                                        target.setType(targetDto.getType());
                                        target.setValue(targetDto.getValue());
                                        target.setOperation(operation);
                                        return target;
                                    })
                            .collect(Collectors.toList());

            operationTargetRepository.saveAll(targets);
        }
    }

    private TradeType calculateTradeType(LocalDate entryDate, LocalDate exitDate) {
        if (entryDate.isBefore(exitDate)) {
            return TradeType.SWING;
        } else {
            return TradeType.DAY;
        }
    }
}
