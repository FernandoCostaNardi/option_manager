import com.olisystem.optionsmanager.dto.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.OperationItemDto;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.Operation;
import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.OptionType;
import com.olisystem.optionsmanager.model.TradeType;
import com.olisystem.optionsmanager.model.User;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationTargetRepository;
import com.olisystem.optionsmanager.repository.OptionSerieRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  // Métodos createOperation e calculateTradeType permanecem iguais

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
      OperationSummaryResponseDto summary =
          OperationSummaryCalculator.calculateSummary(sortedDtos);

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
      OperationSummaryResponseDto summary =
          OperationSummaryCalculator.calculateSummary(dtos);

      // Criar uma nova página
      return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
    }
  }

  // Método auxiliar para ordenar a lista de DTOs
  private List<OperationItemDto> sortDtoList(List<OperationItemDto> dtos, Sort sort) {
    // Implementar a ordenação manual baseada nos campos do Sort
    for (Sort.Order order : sort) {
      String property = order.getProperty();
      boolean isAscending = order.getDirection().isAscending();

      Comparator<OperationItemDto> comparator = createComparator(property, isAscending);
      if (comparator != null) {
        dtos.sort(comparator);
      }
    }

    return dtos;
  }

  // Método para criar um comparador baseado na propriedade
  private Comparator<OperationItemDto> createComparator(String property, boolean isAscending) {
    Comparator<OperationItemDto> comparator = null;

    switch (property) {
      case "optionSeriesCode":
        comparator =
            Comparator.comparing(
                dto ->
                    dto.getOptionSeriesCode() != null ? dto.getOptionSeriesCode().toString() : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "entryDate":
        comparator =
            Comparator.comparing(
                OperationItemDto::getEntryDate, Comparator.nullsLast(LocalDate::compareTo));
        break;
      case "exitDate":
        comparator =
            Comparator.comparing(
                OperationItemDto::getExitDate, Comparator.nullsLast(LocalDate::compareTo));
        break;
      case "entryTotalValue":
        comparator =
            Comparator.comparing(
                OperationItemDto::getEntryTotalValue, Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "exitTotalValue":
        comparator =
            Comparator.comparing(
                OperationItemDto::getExitTotalValue, Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "profitLoss":
        comparator =
            Comparator.comparing(
                OperationItemDto::getProfitLoss, Comparator.nullsLast(BigDecimal::compareTo));
        break;
      case "profitLossPercentage":
        comparator =
            Comparator.comparing(
                OperationItemDto::getProfitLossPercentage,
                Comparator.nullsLast(BigDecimal::compareTo));
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
      case "analysisHouseName":
        comparator =
            Comparator.comparing(
                OperationItemDto::getAnalysisHouseName, Comparator.nullsLast(String::compareTo));
        break;
      case "brokerageName":
        comparator =
            Comparator.comparing(
                OperationItemDto::getBrokerageName, Comparator.nullsLast(String::compareTo));
        break;
      case "optionType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getOptionType, Comparator.nullsLast(Enum::compareTo));
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

    // Filtro por tipo de transação
    if (criteria.getTransactionType() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.get("transactionType"), criteria.getTransactionType()));
    }

    // Filtro por tipo de trade
    if (criteria.getTradeType() != null) {
      spec =
          spec.and((root, query, cb) -> cb.equal(root.get("tradeType"), criteria.getTradeType()));
    }

    // Filtro por tipo de opção
    if (criteria.getOptionType() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.join("optionSeries").get("type"), criteria.getOptionType()));
    }

    // Filtros adicionais para análise e corretora se forem adicionados
    if (criteria.getAnalysisHouseName() != null && !criteria.getAnalysisHouseName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.join("analysisHouse").get("name")),
                      "%" + criteria.getAnalysisHouseName().toLowerCase() + "%"));
    }

    if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.join("brokerage").get("name")),
                      "%" + criteria.getBrokerageName().toLowerCase() + "%"));
    }

    return spec;
  }

  public Page<OperationSummaryResponseDto> findByFilters(
      OperationFilterCriteria criteria, Pageable pageable) {

    // Verificar se há ordenação por campos que não existem na entidade
    if (pageable.getSort().isSorted()) {
      // Verificar se algum dos campos de ordenação é um campo derivado
      boolean hasCustomSort = false;
      for (Sort.Order order : pageable.getSort()) {
        if (order.getProperty().equals("optionSeriesCode")
            || order.getProperty().equals("analysisHouseName")
            || order.getProperty().equals("brokerageName")
            || order.getProperty().equals("baseAssetLogoUrl")
            || order.getProperty().equals("transactionType")
            || order.getProperty().equals("tradeType")
            || order.getProperty().equals("status")
            || order.getProperty().equals("optionType")) {
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
        List<OperationItemDto> dtos =
            page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

        // Ordenar manualmente a lista de DTOs
        List<OperationItemDto> sortedDtos = sortDtoList(dtos, sort);

        // Calcular totalizadores usando a classe utilitária
        OperationSummaryResponseDto summary =
            OperationSummaryCalculator.calculateSummary(sortedDtos);

        // Criar uma nova página com os DTOs ordenados
        return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
      }
    }

    // Se não houver ordenação personalizada, usar o fluxo normal
    Specification<Operation> spec = createSpecification(criteria);
    Page<Operation> operations = operationRepository.findAll(spec, pageable);

    // Mapear para DTOs
    List<OperationItemDto> dtos =
        operations.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

    // Calcular totalizadores
    OperationSummaryResponseDto summary =
        OperationSummaryCalculator.calculateSummary(dtos);

    // Retornar a página de resumo
    return new PageImpl<>(List.of(summary), pageable, operations.getTotalElements());
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
        .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
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
}
