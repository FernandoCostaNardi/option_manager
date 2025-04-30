package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.dto.OperationDataRequest;
import com.olisystem.optionsmanager.dto.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.*;
import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.repository.*;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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
public class OperationService {

  @Autowired private OptionSerieRepository optionSerieRepository;
  @Autowired private OperationRepository operationRepository;
  @Autowired private OperationTargetRepository operationTargetRepository;
  @Autowired private BrokerageService brokerageService;
  @Autowired private AnalysisHouseService analysisHouseService;
  @Autowired private AssetService assetService;
  @Autowired private OptionSerieService optionSerieService;

  public void createOperation(OperationDataRequest request) {
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
      AnalysisHouse analysisHouse =
          analysisHouseService.getAnalysisHouseById(request.getAnalysisHouseId());
      operation.setAnalysisHouse(analysisHouse);
    }

    operation.setTransactionType(request.getTransactionType());
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

      // Converter para DTOs - com tipo explícito Function<Operation, OperationSummaryResponseDto>
      List<OperationSummaryResponseDto> dtos =
          page.getContent().stream()
              .map(
                  (Operation op) -> {
                    return OperationSummaryResponseDto.builder()
                        .id(op.getId())
                        .optionSerieCode(op.getOptionSeries().getCode().toUpperCase())
                        .entryDate(op.getEntryDate())
                        .exitDate(op.getExitDate())
                        .status(op.getStatus())
                        .analysisHouseName(
                            op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
                        .brokerageName(
                            op.getBrokerage() != null ? op.getBrokerage().getName() : null)
                        .quantity(op.getQuantity())
                        .entryUnitPrice(op.getEntryUnitPrice())
                        .entryTotalValue(op.getEntryTotalValue())
                        .exitUnitPrice(op.getExitUnitPrice())
                        .exitTotalValue(op.getExitTotalValue())
                        .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
                        .build();
                  })
              .collect(Collectors.toList());

      // Ordenar manualmente a lista de DTOs
      List<OperationSummaryResponseDto> sortedDtos = sortDtoList(dtos, sort);

      // Criar uma nova página com os DTOs ordenados
      return new PageImpl<>(sortedDtos, pageable, page.getTotalElements());
    } else {
      // Se não houver ordenação, usar o fluxo normal
      User user = SecurityUtil.getLoggedUser();
      Page<Operation> page = operationRepository.findByUserAndStatusIn(user, statuses, pageable);

      // Aplicando a mesma solução com tipo explícito aqui
      return page.map(
          (Operation op) -> {
            return OperationSummaryResponseDto.builder()
                .optionSerieCode(op.getOptionSeries().getCode().toUpperCase())
                .entryDate(op.getEntryDate())
                .exitDate(op.getExitDate())
                .status(op.getStatus())
                .analysisHouseName(
                    op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
                .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
                .quantity(op.getQuantity())
                .entryUnitPrice(op.getEntryUnitPrice())
                .entryTotalValue(op.getEntryTotalValue())
                .exitUnitPrice(op.getExitUnitPrice())
                .exitTotalValue(op.getExitTotalValue())
                .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
                .build();
          });
    }
  }

  // Método auxiliar para ordenar a lista de DTOs
  private List<OperationSummaryResponseDto> sortDtoList(
      List<OperationSummaryResponseDto> dtos, Sort sort) {
    // Implementar a ordenação manual baseada nos campos do Sort
    for (Sort.Order order : sort) {
      String property = order.getProperty();
      boolean isAscending = order.getDirection().isAscending();

      Comparator<OperationSummaryResponseDto> comparator = createComparator(property, isAscending);
      if (comparator != null) {
        dtos.sort(comparator);
      }
    }

    return dtos;
  }

  // Método para criar um comparador baseado na propriedade
  private Comparator<OperationSummaryResponseDto> createComparator(
      String property, boolean isAscending) {
    Comparator<OperationSummaryResponseDto> comparator = null;

    switch (property) {
      case "optionSerieCode":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getOptionSerieCode,
                Comparator.nullsLast(String::compareTo));
        break;
      case "entryDate":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getEntryDate,
                Comparator.nullsLast(LocalDate::compareTo));
        break;
      case "exitDate":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getExitDate,
                Comparator.nullsLast(LocalDate::compareTo));
        break;
      case "entryTotalValue":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getEntryTotalValue,
                Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "exitTotalValue":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getExitTotalValue,
                Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "profitLoss":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getProfitLoss,
                Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "profitLossPercentage":
        comparator =
            Comparator.comparing(
                OperationSummaryResponseDto::getProfitLossPercentage,
                Comparator.nullsLast(BigDecimal::compareTo));
        break;
      default:
        // Propriedade desconhecida, não ordenar
        break;
    }

    // Inverter a ordenação se for descendente
    if (comparator != null && !isAscending) {
      comparator = comparator.reversed();
    }

    return comparator;
  }

  public Page<OperationSummaryResponseDto> findByFilters(
      OperationFilterCriteria criteria, Pageable pageable) {

    // Verificar se há ordenação por campos que não existem na entidade
    if (pageable.getSort().isSorted()) {
      // Verificar se algum dos campos de ordenação é um campo derivado
      boolean hasCustomSort = false;
      for (Sort.Order order : pageable.getSort()) {
        if (order.getProperty().equals("optionSerieCode")
            || order.getProperty().equals("analysisHouseName")
            || order.getProperty().equals("brokerageName")
            || order.getProperty().equals("baseAssetLogoUrl")) {
          hasCustomSort = true;
          break;
        }
      }

      if (hasCustomSort) {
        // Extrair a ordenação original
        Sort sort = pageable.getSort();

        // Criar um novo Pageable sem ordenação
        Pageable pageableWithoutSort =
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        // Aplicar filtros
        Specification<Operation> spec = createSpecification(criteria);

        // Buscar as operações sem ordenação
        Page<Operation> page = operationRepository.findAll(spec, pageableWithoutSort);

        // Converter para DTOs
        List<OperationSummaryResponseDto> dtos =
            page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

        // Ordenar manualmente a lista de DTOs
        List<OperationSummaryResponseDto> sortedDtos = sortDtoList(dtos, sort);

        // Criar uma nova página com os DTOs ordenados
        return new PageImpl<>(sortedDtos, pageable, page.getTotalElements());
      }
    }

    // Se não houver ordenação personalizada, usar o fluxo normal
    Specification<Operation> spec = createSpecification(criteria);
    Page<Operation> operations = operationRepository.findAll(spec, pageable);

    // Mapear para DTOs
    return operations.map(this::mapToDto);
  }

  // Método auxiliar para criar a especificação com base nos critérios
  private Specification<Operation> createSpecification(OperationFilterCriteria criteria) {
    Specification<Operation> spec = Specification.where(null);

    // Adicionar usuário logado ao filtro
    User user = SecurityUtil.getLoggedUser();
    spec = spec.and((root, query, cb) -> cb.equal(root.get("user"), user));

    // Aplicar filtros se fornecidos
    if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
      spec = spec.and((root, query, cb) -> root.get("status").in(criteria.getStatus()));
    }

    // Filtros de data de entrada
    if (criteria.getEntryDateStart() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.greaterThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateStart()));
    }

    if (criteria.getEntryDateEnd() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.lessThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateEnd()));
    }

    // Filtros de data de saída
    if (criteria.getExitDateStart() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.greaterThanOrEqualTo(root.get("exitDate"), criteria.getExitDateStart()));
    }

    if (criteria.getExitDateEnd() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.lessThanOrEqualTo(root.get("exitDate"), criteria.getExitDateEnd()));
    }

    if (criteria.getAnalysisHouseName() != null && !criteria.getAnalysisHouseName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("analysisHouse").get("name")),
                      "%" + criteria.getAnalysisHouseName().toLowerCase() + "%"));
    }

    if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("brokerage").get("name")),
                      "%" + criteria.getBrokerageName().toLowerCase() + "%"));
    }

    return spec;
  }

  // Método auxiliar para mapear Operation para DTO
  private OperationSummaryResponseDto mapToDto(Operation op) {
    return OperationSummaryResponseDto.builder()
        .optionSerieCode(op.getOptionSeries().getCode())
        .entryDate(op.getEntryDate())
        .exitDate(op.getExitDate())
        .status(op.getStatus())
        .analysisHouseName(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
        .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
        .quantity(op.getQuantity())
        .entryUnitPrice(op.getEntryUnitPrice())
        .entryTotalValue(op.getEntryTotalValue())
        .exitUnitPrice(op.getExitUnitPrice())
        .exitTotalValue(op.getExitTotalValue())
        .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
        .build();
  }

  // Método para ordenar DTOs manualmente
  private List<OperationSummaryResponseDto> sortDtos(
      List<OperationSummaryResponseDto> dtos, Sort sort) {
    for (Sort.Order order : sort) {
      String property = order.getProperty();
      boolean isAscending = order.getDirection().isAscending();

      Comparator<OperationSummaryResponseDto> comparator = null;

      switch (property) {
        case "optionSerieCode":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getOptionSerieCode,
                  Comparator.nullsLast(String::compareTo));
          break;
        case "entryDate":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getEntryDate,
                  Comparator.nullsLast(LocalDate::compareTo));
          break;
        case "exitDate":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getExitDate,
                  Comparator.nullsLast(LocalDate::compareTo));
          break;
        case "analysisHouseName":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getAnalysisHouseName,
                  Comparator.nullsLast(String::compareTo));
          break;
        case "brokerageName":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getBrokerageName,
                  Comparator.nullsLast(String::compareTo));
          break;
        case "entryTotalValue":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getEntryTotalValue,
                  Comparator.nullsLast(BigDecimal::compareTo));
          break;
        case "exitTotalValue":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getExitTotalValue,
                  Comparator.nullsLast(BigDecimal::compareTo));
          break;
        case "profitLoss":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getProfitLoss,
                  Comparator.nullsLast(BigDecimal::compareTo));
          break;
        case "profitLossPercentage":
          comparator =
              Comparator.comparing(
                  OperationSummaryResponseDto::getProfitLossPercentage,
                  Comparator.nullsLast(BigDecimal::compareTo));
          break;
        default:
          // Propriedade desconhecida, não ordenar
          break;
      }

      if (comparator != null) {
        if (!isAscending) {
          comparator = comparator.reversed();
        }

        final Comparator<OperationSummaryResponseDto> finalComparator = comparator;
        dtos = dtos.stream().sorted(finalComparator).collect(Collectors.toList());
      }
    }

    return dtos;
  }
}
