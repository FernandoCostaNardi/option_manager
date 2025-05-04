package com.olisystem.optionsmanager.service.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.mapper.operation.OperationMapper;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.report.OperationReportData;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationTargetRepository;
import com.olisystem.optionsmanager.repository.OptionSerieRepository;
import com.olisystem.optionsmanager.service.analysis_house.AnalysisHouseService;
import com.olisystem.optionsmanager.service.asset.AssetService;
import com.olisystem.optionsmanager.service.brokerage.BrokerageService;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import com.olisystem.optionsmanager.validation.OperationValidator;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OperationService implements OperationReportData {

  @Autowired private OperationRepository operationRepository;
  @Autowired private AssetService assetService;
  @Autowired private OptionSerieService optionSerieService;
  @Autowired private OptionSerieRepository optionSerieRepository;
  @Autowired private AnalysisHouseService analysisHouseService;
  @Autowired private BrokerageService brokerageService;
  @Autowired private OperationTargetRepository operationTargetRepository;
  @Autowired private OperationValidator operationValidator;
  @Autowired private OperationMapper operationMapper;

  public void createOperation(OperationDataRequest request) {
    operationValidator.validateCreate(request);

    if (request.getId() != null) {
      updateOperation(request.getId(), request);
      return;
    }

    log.info("indo buscar o : " + request.getBaseAssetCode());
    // 1. Busca ou salva o Asset
    Asset asset = assetService.getAssetByCode(request.getBaseAssetCode());
    log.info("Asset buscou e encontrou: " + asset);
    if (asset == null) {
      asset = new Asset();
      asset.setCode(request.getBaseAssetCode().toLowerCase());
      asset.setName(request.getBaseAssetName());
      asset.setType(request.getBaseAssetType());
      asset.setUrlLogo(request.getBaseAssetLogoUrl());
      asset = assetService.save(asset);
    }

    // 2. Busca ou salva o OptionSerie
    OptionSerie optionSerie =
        optionSerieService.getOptionSerieByCode(request.getOptionSeriesCode());
    if (optionSerie == null) {
      optionSerie = new OptionSerie();
      optionSerie.setCode(request.getOptionSeriesCode().toUpperCase());
      optionSerie.setType(request.getOptionSeriesType());
      optionSerie.setStrikePrice(request.getOptionSeriesStrikePrice());
      optionSerie.setExpirationDate(request.getOptionSeriesExpirationDate());
      optionSerie.setAsset(asset);
      optionSerie = optionSerieRepository.save(optionSerie);
    }

    // 3. Salva Operation
    Operation operation = new Operation();
    operation.setOptionSeries(optionSerie);
    Brokerage brokerage = brokerageService.getBrokerageById(request.getBrokerageId());
    operation.setBrokerage(brokerage);

    if (request.getAnalysisHouseId() != null) {
      Optional<AnalysisHouse> analysisHouseOpt =
          analysisHouseService.findById(request.getAnalysisHouseId());
      analysisHouseOpt.ifPresent(operation::setAnalysisHouse);
    }

    operation.setTransactionType(request.getTransactionType());
    if (request.getExitDate() != null) {
      operation.setTradeType(calculateTradeType(request.getEntryDate(), request.getExitDate()));
    }
    operation.setEntryDate(request.getEntryDate());
    operation.setExitDate(request.getExitDate());
    operation.setQuantity(request.getQuantity());
    operation.setEntryUnitPrice(request.getEntryUnitPrice());
    operation.setEntryTotalValue(
        request.getEntryUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
    operation.setStatus(OperationStatus.ACTIVE);
    operation.setUser(SecurityUtil.getLoggedUser());

    // Salva a Operation e guarda a referência final
    final Operation savedOperation = operationRepository.save(operation);

    // 4. Salva OperationTargets
    List<OperationTarget> targets = request.getTargets();
    if (targets != null && !targets.isEmpty()) {
      List<OperationTarget> newTargets =
          targets.stream()
              .map(
                  targetDto -> {
                    OperationTarget target = new OperationTarget();
                    target.setSequence(targetDto.getSequence());
                    target.setType(targetDto.getType());
                    target.setValue(targetDto.getValue());
                    target.setOperation(savedOperation); // Usando a referência final
                    return target;
                  })
              .collect(Collectors.toList());

      operationTargetRepository.saveAll(newTargets);
    }
  }

  private TradeType calculateTradeType(LocalDate entryDate, LocalDate exitDate) {
    if (entryDate.isBefore(exitDate)) {
      return TradeType.SWING;
    } else {
      return TradeType.DAY;
    }
  }

  public Page<OperationSummaryResponseDto> findByStatuses(
      List<OperationStatus> statuses, Pageable pageable) {
    // Verificar se há ordenação por campos que não existem na entidade
    if (pageable.getSort().isSorted()) {
      // Extrair a ordenação original
      Sort sort = pageable.getSort();

      // Criar um novo Pageable sem ordenação
      Pageable pageableWithoutSort =
          PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

      // Buscar as operações do usuário logado
      User user = SecurityUtil.getLoggedUser();
      Page<Operation> page =
          operationRepository.findByUserAndStatusIn(user, statuses, pageableWithoutSort);

      // Converter para DTOs
      List<OperationItemDto> dtos =
          page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

      // Ordenar manualmente a lista de DTOs
      List<OperationItemDto> sortedDtos = sortDtoList(dtos, sort);

      // Calcular totalizadores usando a classe utilitária
      OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(sortedDtos);

      // Criar uma nova página com os DTOs ordenados
      return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
    } else {
      // Se não houver ordenação, usar o fluxo normal
      User user = SecurityUtil.getLoggedUser();
      Page<Operation> page = operationRepository.findByUserAndStatusIn(user, statuses, pageable);

      // Converter para DTOs
      List<OperationItemDto> dtos =
          page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

      // Calcular totalizadores usando a classe utilitária
      OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(dtos);

      // Criar uma nova página
      return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
    }
  }

  // Método auxiliar para ordenar a lista de DTOs
  private List<OperationItemDto> sortDtoList(List<OperationItemDto> dtos, Sort sort) {
    List<OperationItemDto> sortedDtos = dtos;
    for (Sort.Order order : sort) {
      String property = order.getProperty();
      boolean isAscending = order.getDirection().isAscending();
      sortedDtos.sort(createComparator(property, isAscending));
    }
    return sortedDtos;
  }

  // Método para criar um comparador baseado na propriedade
  private Comparator<OperationItemDto> createComparator(String property, boolean isAscending) {
    Comparator<OperationItemDto> comparator;
    switch (property) {
      case "optionSeriesCode": // Certifique-se de que o nome está correto aqui
        comparator =
            Comparator.comparing(
                OperationItemDto::getOptionSeriesCode, String.CASE_INSENSITIVE_ORDER);
        break;
      case "analysisHouseName":
        comparator =
            Comparator.comparing(
                dto ->
                    dto.getAnalysisHouseName() != null
                        ? dto.getAnalysisHouseName().toLowerCase()
                        : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "brokerageName":
        comparator =
            Comparator.comparing(
                dto -> dto.getBrokerageName() != null ? dto.getBrokerageName().toLowerCase() : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "baseAssetLogoUrl":
        comparator =
            Comparator.comparing(
                dto -> dto.getBaseAssetLogoUrl() != null ? dto.getBaseAssetLogoUrl() : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "transactionType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getTransactionType, Comparator.nullsLast(Enum::compareTo));
        break;
      case "tradeType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getTradeType, Comparator.nullsLast(Enum::compareTo));
        break;
      case "status":
        comparator =
            Comparator.comparing(
                OperationItemDto::getStatus, Comparator.nullsLast(Enum::compareTo));
        break;
      case "optionType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getOptionType, Comparator.nullsLast(Enum::compareTo));
        break;
      default:
        // Para outras propriedades, tentar usar reflection
        comparator = (o1, o2) -> 0; // Comparador neutro como fallback
    }
    return isAscending ? comparator : comparator.reversed();
  }

  @Override
  public OperationSummaryResponseDto findByFilters(
      OperationFilterCriteria criteria, Pageable pageable) {
    Page<Operation> page = operationRepository.findAll(createSpecification(criteria), pageable);
    List<OperationItemDto> dtos =
        page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

    OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(dtos);
    return summary;
  }

  private Specification<Operation> createSpecification(OperationFilterCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro por usuário
      predicates.add(cb.equal(root.get("user"), SecurityUtil.getLoggedUser()));

      // Filtro por status
      if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
        predicates.add(root.get("status").in(criteria.getStatus()));
      }

      // Filtro por datas
      if (criteria.getEntryDateStart() != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateStart()));
      }
      if (criteria.getEntryDateEnd() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateEnd()));
      }
      if (criteria.getExitDateStart() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("exitDate"), criteria.getExitDateStart()));
      }
      if (criteria.getExitDateEnd() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("exitDate"), criteria.getExitDateEnd()));
      }

      // Filtro por casa de análise
      if (criteria.getAnalysisHouseName() != null && !criteria.getAnalysisHouseName().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("analysisHouse").get("name")),
                "%" + criteria.getAnalysisHouseName().toLowerCase() + "%"));
      }

      // Filtro por corretora
      if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("brokerage").get("name")),
                "%" + criteria.getBrokerageName().toLowerCase() + "%"));
      }

      // Filtro por tipo de transação
      if (criteria.getTransactionType() != null) {
        predicates.add(cb.equal(root.get("transactionType"), criteria.getTransactionType()));
      }

      // Filtro por tipo de trade
      if (criteria.getTradeType() != null) {
        predicates.add(cb.equal(root.get("tradeType"), criteria.getTradeType()));
      }

      // Filtro por tipo de opção
      if (criteria.getOptionType() != null) {
        predicates.add(cb.equal(root.get("optionSeries").get("type"), criteria.getOptionType()));
      }

      // Filtro por código da série
      if (criteria.getOptionSeriesCode() != null && !criteria.getOptionSeriesCode().isEmpty()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("optionSeries").get("code")),
                "%" + criteria.getOptionSeriesCode().toLowerCase() + "%"));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private OperationItemDto mapToDto(Operation op) {
    return OperationItemDto.builder()
        .id(op.getId())
        .optionSeriesCode(
            op.getOptionSeries()
                .getCode()
                .toUpperCase()) // Convertendo para maiúsculas para manter consistência
        .optionType(op.getOptionSeries().getType())
        .transactionType(op.getTransactionType())
        .tradeType(op.getTradeType())
        .entryDate(op.getEntryDate())
        .exitDate(op.getExitDate())
        .status(op.getStatus())
        .analysisHouseName(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
        .analysisHouseId(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getId() : null)
        .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
        .brokerageId(op.getBrokerage() != null ? op.getBrokerage().getId() : null)
        .quantity(op.getQuantity())
        .entryUnitPrice(op.getEntryUnitPrice())
        .entryTotalValue(op.getEntryTotalValue())
        .exitUnitPrice(op.getExitUnitPrice())
        .exitTotalValue(op.getExitTotalValue())
        .profitLoss(op.getProfitLoss())
        .profitLossPercentage(op.getProfitLossPercentage())
        .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
        .build();
  }

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
}
